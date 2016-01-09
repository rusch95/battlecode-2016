package role_task_abstractionbot2.tasks;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import role_task_abstractionbot2.Task;

public class NonTurretDerp implements Task {
    Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    private Random rand;
    private int myAttackRange;
    private Team myTeam;
    private Team enemyTeam;
    private RobotController rc;
    static int[] tryDirections = {0,-1,1,-2,2};
	
	public NonTurretDerp(RobotController rc) {
		this.rc = rc;
    	this.rand = new Random(rc.getID());
    	this.myTeam = rc.getTeam();
    	this.enemyTeam = myTeam.opponent();
        this.myAttackRange = rc.getType().attackRadiusSquared;   
	}
	
	public static RobotInfo getWeakestRobot(RobotInfo[] nearbyRobots) {
		//Returns weakest unit from an array of sensed robots
		if (nearbyRobots.length > 0) {
        	double minHealth = Double.POSITIVE_INFINITY;
        	RobotInfo weakestBot = null;
        	for (RobotInfo curBot : nearbyRobots){
        		//Iterating through to find weakest robot
        		if (curBot.health < minHealth) {
        			minHealth = curBot.health;
        			weakestBot = curBot;
        		}
        	return weakestBot;
        	}
		}
		return null;
	}
	
	public static int getNumberOfBotOfType(RobotInfo[] nearbyRobots, RobotType botType) {
		int numberOf = 0;
		for (RobotInfo bot : nearbyRobots) {
			if (bot.type == botType) {
				numberOf += 1;
			}
		}
		return numberOf;
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

        boolean shouldAttack = false;
        Direction dirToMove = Direction.NONE;

        // If this robot type can attack, check for enemies within range and attack one
        RobotInfo[] enemiesSeen = rc.senseHostileRobots(rc.getLocation(), -1);
        if (enemiesSeen.length > 0) {
            RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, enemyTeam);
            RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
            if (enemiesWithinRange.length > 0) {
                shouldAttack = true;
                // Check if weapon is ready
                if (rc.getType() == RobotType.TTM && fate < 250) {
                	rc.unpack();
                }
                else if (rc.isWeaponReady()) {
                    rc.attackLocation(getWeakestRobot(enemiesWithinRange).location);
                }
            } else if (zombiesWithinRange.length > 0) {
                shouldAttack = true;
                // Check if weapon is ready
                if (rc.getType() == RobotType.TTM && fate < 250) {
                	rc.unpack();
                }
                else if (rc.isWeaponReady()) {
                    rc.attackLocation(getWeakestRobot(zombiesWithinRange).location);
                }
            } else {
            	RobotInfo weakestEnemy = enemiesSeen[0];
            	if (weakestEnemy != null)
            		dirToMove = rc.getLocation().directionTo(weakestEnemy.location);
            	
            }
        } else {
        	RobotInfo[] friendsSeen = rc.senseNearbyRobots(-1, rc.getTeam());
        	RobotInfo weakestFriend = getWeakestRobot(friendsSeen);
        	if (weakestFriend != null) {
        		if (weakestFriend.health  < weakestFriend.maxHealth)
        			dirToMove = rc.getLocation().directionTo(weakestFriend.location);
        	}
        	
        }
        

        if (dirToMove != Direction.NONE) {
        	tryToMove(dirToMove);
        } else if (!shouldAttack) {
        	if (rc.isCoreReady()) {
            	if (rc.senseNearbyRobots(5, rc.getTeam()).length > 4) {
	               
	                // Choose a random direction to try to move in
            		if (dirToMove == Direction.NONE)
            			dirToMove = directions[fate % 8];
            		tryToMove(dirToMove);
                }
            }
        } else if (rc.getType() == RobotType.TTM && fate < 250) {
        	rc.unpack();
        }
        return 1;
	}

}
