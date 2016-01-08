package role_task_abstractionbot2.tasks;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import role_task_abstractionbot2.Task;

public class ArchonDerp implements Task {
    Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    RobotType[] robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET};
    private Random rand;
    private RobotController rc;
    private int sightRange;
	
	public ArchonDerp(RobotController rc) {
		this.rc = rc;
    	this.rand = new Random(rc.getID());
    	this.sightRange = RobotType.TURRET.sensorRadiusSquared;
    	
	}
	
	public static RobotInfo getWeakestRobot(RobotInfo[] nearbyRobots) {
		//Returns weakest unit from an array of sensed robots
		if (nearbyRobots.length > 0) {
        	double minHealth = Double.POSITIVE_INFINITY;
        	RobotInfo weakestBot = null;
        	for (RobotInfo curBot : nearbyRobots){
        		//Iterating through to find weakest robot
        		if (curBot.health < minHealth && curBot.type != RobotType.ARCHON) {
        			minHealth = curBot.health;
        			weakestBot = curBot;
        		}
        	return weakestBot;
        	}
		}
		return null;
	}
	
	@Override
	public int run() throws GameActionException {
        int fate = rand.nextInt(1000);
        // Check if this ARCHON's core is ready
        
        
        if (rc.isCoreReady()) {
        	final MapLocation[] sensedSquares = rc.getLocation().getAllMapLocationsWithinRadiusSq(rc.getLocation(), sightRange);
        	MapLocation partsLocation = null;
        	for (MapLocation location : sensedSquares) {
        		if (rc.senseParts(location) > 0) {
        			partsLocation = location;
        		}
        	}
        	RobotInfo[] enemies = rc.senseHostileRobots(rc.getLocation(), -1);
        	if (enemies != null && fate % 3 < 3) {
        		if (enemies.length > 0) {
        			MapLocation enemyLoc = enemies[0].location;
        			Direction dirToMove = enemyLoc.directionTo(rc.getLocation());
        			if (rc.canMove(dirToMove)) {
        				rc.move(dirToMove);
        			}
        		}
        	}
        	
            if (partsLocation != null && fate < 500) {
                // Choose a random direction to try to move in
                Direction dirToMove = rc.getLocation().directionTo(partsLocation);
                // Check the rubble in that direction
                if (rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
                    // Too much rubble, so I should clear it
                    rc.clearRubble(dirToMove);
                    // Check if I can move in this direction
                } else if (rc.canMove(dirToMove)) {
                    // Move
                    rc.move(dirToMove);
                }
            } else if (fate < 333) {
                // Choose a random unit to build
                RobotType typeToBuild = robotTypes[fate % 8];
                //typeToBuild = RobotType.SOLDIER;
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
