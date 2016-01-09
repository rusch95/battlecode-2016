package cheeseBot.tasks;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import cheeseBot.Task;

public class ArchonDerp implements Task {
    Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    RobotType[] robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET};
    private Random rand;
    private RobotController rc;
    private int sightRange;
    static int[] tryDirections = {0,-1,1,-2,2};
    private MapLocation partsLocation = null;
	
	public ArchonDerp(RobotController rc) {
		this.rc = rc;
    	this.rand = new Random(rc.getID());
    	this.sightRange = RobotType.TURRET.sensorRadiusSquared;
    	
	}
	
	public static RobotInfo getWeakestRobot(RobotInfo[] nearbyRobots) {
		//Returns weakest unit from an array of sensed robots
		RobotInfo weakestBot = null;
		if (nearbyRobots.length > 0) {
        	double minHealth = Double.POSITIVE_INFINITY;
        	for (RobotInfo curBot : nearbyRobots){
        		//Iterating through to find weakest robot
        		if (curBot.health < minHealth && curBot.type != RobotType.ARCHON) {
        			minHealth = curBot.health;
        			weakestBot = curBot;
        		}
        	
        	}
		}
		return weakestBot;
	}
	
	public RobotInfo getClosestRobot(RobotInfo[] nearbyRobots) {
		//Returns weakest unit from an array of sensed robots
		if (nearbyRobots.length > 0) {
        	int distance = Integer.MAX_VALUE;
        	RobotInfo closestBot = null;
        	int curDistance = 0;
        	for (RobotInfo curBot : nearbyRobots){
        		//Iterating through to find closest robot
        		curDistance = curBot.location.distanceSquaredTo(rc.getLocation());
        		if (curDistance < distance && curBot.type != RobotType.ARCHON) {
        			distance = curDistance;
        			closestBot = curBot;
        		}
        	return closestBot;
        	}
		}
		return null;
	}
	
	public void tryToMove(Direction forward) throws GameActionException{
		if(rc.isCoreReady()){
			for(int deltaD:tryDirections){
				Direction maybeForward = Direction.values()[(forward.ordinal()+deltaD+8)%8];
				if(rc.canMove(maybeForward)){
					rc.move(maybeForward);
					return;
				}
			}
			if(rc.getType().canClearRubble()){
				//failed to move, look to clear rubble
				MapLocation ahead = rc.getLocation().add(forward);
				if(rc.senseRubble(ahead)>=GameConstants.RUBBLE_OBSTRUCTION_THRESH){
					rc.clearRubble(forward);
				}
			}
		}
	}
	
	@Override
	public int run() throws GameActionException {
        int fate = rand.nextInt(1000);
        
        
        // Heal a bitch
        RobotInfo weakestFriend = getWeakestRobot(rc.senseNearbyRobots(rc.getType().attackRadiusSquared, rc.getTeam()));
        if (weakestFriend != null) {
        	rc.repair(weakestFriend.location);
        }
        // Check if this ARCHON's core is ready
        if (rc.isCoreReady()) {
        	
        	RobotInfo[] enemies = rc.senseHostileRobots(rc.getLocation(), 25);
        	rc.setIndicatorString(1,String.valueOf(partsLocation));
        	
        	if (partsLocation == null && fate < 50) {
	        	@SuppressWarnings("static-access")
				final MapLocation[] sensedSquares = rc.getLocation().getAllMapLocationsWithinRadiusSq(rc.getLocation(), sightRange);
	        	for (MapLocation location : sensedSquares) {
	        		if (rc.senseParts(location) > 0) {
	        			partsLocation = location;
	        			break;
	        		}
	        	}
        	}
        	
            if (partsLocation != null && fate % 7 == 1) {
            	// Get direction towards parts if seen
            	Direction dirToMove = rc.getLocation().directionTo(partsLocation);
            	tryToMove(dirToMove);
            	if (rc.senseParts(rc.getLocation()) > 0 || (fate % 10 == 1)) {
            		// Grabbed parts, so empty partsLocation of this spot
            		partsLocation = null;
            	}
            } else if (enemies != null && (fate % 3 == 3)) {
            	// Gets close enemies and tries to run away from one
        		if (enemies.length > 0) {
        			MapLocation enemyLoc = enemies[0].location;
        			if (enemyLoc != null) {
        				Direction dirToMove = enemyLoc.directionTo(rc.getLocation());
        				tryToMove(dirToMove);
        			}
        		}
            } else if (fate < 333 && rc.isCoreReady()) {
                // Choose a random unit to build
            	RobotType typeToBuild = null;
            	if (rc.getRobotCount() < 40) {
                	typeToBuild = robotTypes[fate % 8];
            	}
            	else {
            		typeToBuild = RobotType.TURRET;
            	}
                // Check for sufficient parts
                if (rc.hasBuildRequirements(typeToBuild)) {
                    // Choose a random direction to try to build in
                    Direction dirToBuild = directions[rand.nextInt(8)];
                    for (int i = 0; i < 8; i++) {
                        // If possible, build in this direction
                        if (rc.canBuild(dirToBuild, typeToBuild)) {
                            rc.build(dirToBuild, typeToBuild);
                            break;
                        } else {
                            // Rotate the direction to try
                            dirToBuild = dirToBuild.rotateLeft();
                        }
                    }
                }
            }
        }
        return 0;
	}

}
