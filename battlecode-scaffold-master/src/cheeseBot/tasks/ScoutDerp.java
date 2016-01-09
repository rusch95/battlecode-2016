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

public class ScoutDerp implements Task {
    Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    private Random rand;
    private Team myTeam;
    private Team enemyTeam;
    private RobotController rc;
    private int sightRange;
    static int[] tryDirections = {0,-1,1,-2,2};
    private MapLocation spotArchon = null;
	
	public ScoutDerp(RobotController rc) {
		this.rc = rc;
    	this.rand = new Random(rc.getID());
    	this.myTeam = rc.getTeam();
    	this.enemyTeam = myTeam.opponent();
    	this.sightRange = RobotType.SCOUT.sensorRadiusSquared;
	}

	@Override
	public int run() throws GameActionException {
		int fate = rand.nextInt(1000);

        Direction dirToMove = Direction.NONE;

        RobotInfo[] allEnemies = rc.senseHostileRobots(rc.getLocation(), -1);
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, enemyTeam);
        
        if (enemies.length > 0) {
        	dirToMove = rc.getLocation().directionTo((enemies[fate % enemies.length].location));
        	helperMeth.tryToMove(dirToMove, rc);
        	for (RobotInfo e : allEnemies) {
        		if (e.type == RobotType.ARCHON && rc.getRoundNum() > 100) {
        			spotArchon = e.location;
        			rc.broadcastMessageSignal(spotArchon.x, spotArchon.y, 3000);
        			break;
        		} //else if (e.type == RobotType.ZOMBIEDEN) {
        		//	spotArchon = e.location;
        		//	rc.broadcastMessageSignal(spotArchon.x, spotArchon.y, 1000);
        		//}
        	}
		} else {
        	if (rc.isCoreReady()) {
                // Choose a random direction to try to move in
        		dirToMove = directions[fate % 8];
                // Check the rubble in that direction
        		helperMeth.tryToMove(dirToMove, rc);
            }
        }
        return 2;
	}

}
