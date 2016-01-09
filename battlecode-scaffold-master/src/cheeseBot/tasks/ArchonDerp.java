package cheeseBot.tasks;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import cheeseBot.Task;
import cheeseBot.helperMeth;

public class ArchonDerp implements Task {

	Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    RobotType[] robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.GUARD, RobotType.GUARD, RobotType.TURRET, RobotType.TURRET};
    private Random rand;
    private RobotController rc;
    private int sightRange;
    Direction dirToMove = Direction.NONE;
    static int[] tryDirections = {0,-1,1,-2,2};
    private MapLocation partsLocation = null;
    //This is within which enemies are ran away from
    static int scaredRangeSquared = 25;
    //If number units higher than number, spawn only turrets
    static int onlyTurretSpawnNumber = 90;
    //Spawn only scouts until this round
    static int onlyScoutsTilRound = 30;
	
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
	
	public void buildRobot(RobotType typeToBuild, RobotController rc) throws GameActionException{
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
	
	@Override
	public int run() throws GameActionException {
        int fate = rand.nextInt(1000);
        
        
        // Heal a bitch using archon specific get weakest that doesn't consider archons
        // TODO figure out if this is working correctly
        RobotInfo weakestFriend = getWeakestRobot(rc.senseNearbyRobots(rc.getType().attackRadiusSquared, rc.getTeam()));
        if (weakestFriend != null) {
        	rc.repair(weakestFriend.location);
        }
        
        // Check if this ARCHON's core is ready
        if (rc.isCoreReady()) {
        	
        	
        	RobotInfo[] enemies = rc.senseHostileRobots(rc.getLocation(), scaredRangeSquared);
        	
        	// If a partsLocation isn't being consider, chance of searching for parts within sensor range
        	if (partsLocation == null && fate < 250) {
	        	@SuppressWarnings("static-access")
				final MapLocation[] sensedSquares = rc.getLocation().getAllMapLocationsWithinRadiusSq(rc.getLocation(), sightRange);
	        	double maxParts = 0;
	        	for (MapLocation location : sensedSquares) {
	        		if (rc.senseParts(location) > maxParts) {
	        			maxParts = rc.senseParts(location);
	        			partsLocation = location;
	        			break;
	        		}
	        	}
        	}
        	
        	RobotInfo[] nearbyMuscle = rc.senseNearbyRobots(-1, rc.getTeam());
        
        	// If a parts location has been found move towards it
            if (partsLocation != null && fate > 700) {
            	// Get direction towards parts if seen
            	dirToMove = rc.getLocation().directionTo(partsLocation);
            	helperMeth.tryToMoveOre(dirToMove, rc);
            	if (partsLocation.isAdjacentTo(rc.getLocation())) {
            		partsLocation = null;
            	}
            } else if (enemies != null && (fate % 2 == 1)) {
            	// Gets close enemies and tries to run away from one
        		if (enemies.length > 0) {
        			MapLocation enemyLoc = enemies[0].location;
        			if (enemyLoc != null) {
        				dirToMove = enemyLoc.directionTo(rc.getLocation());
        				helperMeth.tryToMove(dirToMove, rc);
        			}
        		}
            } else if (fate < 333 && rc.isCoreReady()) {
                // Choose a random unit to build
            	// TODO figure out why exception without isCoreReadye
            	RobotType typeToBuild = null;
            	boolean zombiesAh = (rc.senseNearbyRobots(25, Team.ZOMBIE)).length > 1;
            	// Check for zombies
            	if (rc.getRoundNum() < onlyScoutsTilRound) {
            		typeToBuild = RobotType.SCOUT;
            	// Build more expensive turrets when more than 90 units
            	} else if (zombiesAh) {	
            		typeToBuild = RobotType.GUARD;
            	} else if (rc.getRobotCount() < onlyTurretSpawnNumber) {
                	typeToBuild = robotTypes[fate % 8];
            	}
            	else {
            		typeToBuild = RobotType.TURRET;
            	}
                // Check for sufficient parts
                buildRobot(typeToBuild, rc);
            } else if (fate > 800) {
            	
            	dirToMove = null;
            	for (RobotInfo muscle : nearbyMuscle) {
            		if (muscle.type == RobotType.ARCHON || muscle.type == RobotType.SCOUT)
            			continue;
            		dirToMove = rc.getLocation().directionTo(nearbyMuscle[0].location);
            	} if (dirToMove == null) {
            		dirToMove = directions[fate % 8];
            	}
        		helperMeth.tryToMove(dirToMove, rc);
            }
        }
        return 0;
	}

}
