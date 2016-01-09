package cheeseBot.tasks;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;
import cheeseBot.Task;
import cheeseBot.helperMeth;

public class NonTurretDerp implements Task {
    Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    private Random rand;
    private int myAttackRange;
    private Team myTeam;
    private Team enemyTeam;
    private RobotController rc;
    private Direction dirToMove = Direction.NONE;
    static int[] tryDirections = {0,-1,1,-2,2};
    private MapLocation spotArchon = null;
	
	public NonTurretDerp(RobotController rc) {
		this.rc = rc;
    	this.rand = new Random(rc.getID());
    	this.myTeam = rc.getTeam();
    	this.enemyTeam = myTeam.opponent();
        this.myAttackRange = rc.getType().attackRadiusSquared;   
	}

	@Override
	public int run() throws GameActionException {
		int fate = rand.nextInt(1000);

        boolean shouldAttack = false;
        dirToMove = Direction.NONE;
        
        Signal[] signalQueue = rc.emptySignalQueue();
        
        // If this robot type can attack, check for enemies within range and attack one
        RobotInfo[] enemiesSeen = rc.senseHostileRobots(rc.getLocation(), -1);
        if (enemiesSeen.length > 0) {
            RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, enemyTeam);
            RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
            if (enemiesWithinRange.length > 0) {
                // Check if weapon is ready
                if (rc.isWeaponReady()) {
                    rc.attackLocation(helperMeth.getWeakestRobot(enemiesWithinRange).location);
                }
            } else if (zombiesWithinRange.length > 0) {
                if (rc.isWeaponReady()) {
                    rc.attackLocation(helperMeth.getWeakestRobot(zombiesWithinRange).location);
                }
            } else {
            	RobotInfo weakestEnemy = enemiesSeen[0];
            	if (weakestEnemy != null)
            		dirToMove = rc.getLocation().directionTo(weakestEnemy.location);
            		helperMeth.tryToMove(dirToMove, rc);           	
            }
        } else if (signalQueue.length > 0) {
        	int x = signalQueue[0].getMessage()[0];
        	int y = signalQueue[0].getMessage()[1];
        	MapLocation gotoLoc = new MapLocation(x,y);
        	dirToMove = rc.getLocation().directionTo(gotoLoc); 
        	
        } else {
        	RobotInfo[] friendsSeen = rc.senseNearbyRobots(-1, rc.getTeam());
        	RobotInfo weakestFriend = helperMeth.getWeakestRobot(friendsSeen);
        	if (weakestFriend != null) {
        		if (weakestFriend.weaponDelay > 1)
        			dirToMove = rc.getLocation().directionTo(weakestFriend.location);
        			helperMeth.tryToMove(dirToMove, rc);
        	}
        }
    	if (rc.isCoreReady()) {
    		//TODO change from magic numbers
    		RobotInfo[] nearby = rc.senseNearbyRobots(16, rc.getTeam());
        	if (nearby.length > 10 || fate < 150){             
                // Choose a random direction to try to move in
        		dirToMove = directions[fate % 8];
        		helperMeth.tryToMove(dirToMove, rc);
            } else if (nearby.length < 2 && nearby.length != 0 && fate % 3 == 2 && rc.getType() == RobotType.SOLDIER) {
            	dirToMove = rc.getLocation().directionTo(nearby[0].location);
            	helperMeth.tryToMove(dirToMove, rc);
            }
        }
        return 1;
	}
}
