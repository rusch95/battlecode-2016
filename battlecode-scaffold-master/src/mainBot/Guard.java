package mainBot;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Guard implements Role {
	private final RobotController rc;
	private final Random rand;
    private final Team myTeam;
    private final Team otherTeam;
    private final MapLocation base;
    private Direction prevDirection = Direction.NONE;
    
    //Magic Numbers
    private final int CLOSE_RANGE = 5;
    private final int MED_RANGE = 25;
    private final int FAR_RANGE = 30;
    private final int MAX_RANGE = -1;
	private final int CLOSE_TOO_MANY = 6;
	private final int CLOSE_TOO_FEW = 2;
	private final int MED_TOO_MANY = 20;
	private final int MED_TOO_FEW = 5;
	private final int FAR_TOO_MANY = 999;
	private final int FAR_TOO_FEW = 0;
	private final int MIN_SQUAD_NUM = 1;
    private final double RETREAT_HEALTH_PERCENT = 0.05;
	
	public Guard(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
		this.base = rc.getLocation();
		//TODO Have this update if the archons change position
	}
	
	@Override
	public void run() {
		while(true){
			try {
				RobotInfo[] enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), RobotType.GUARD.attackRadiusSquared);
				RobotInfo[] enemiesSeen = rc.senseHostileRobots(rc.getLocation(), MAX_RANGE);
				RobotInfo[] friendsSeen = rc.senseNearbyRobots(MAX_RANGE, myTeam);
				if(enemiesWithinRange.length > 0 && rc.isWeaponReady()) { //We're in combat
					RobotInfo targetEnemy = Utility.getTarget(enemiesWithinRange, 0, rc.getLocation());
					if(targetEnemy != null) {
						rc.attackLocation(targetEnemy.location);
					} 
				}
				
				//Flee code
				if (rc.getHealth() / rc.getType().maxHealth < RETREAT_HEALTH_PERCENT) {
					Direction dirToGo = Direction.NONE;
					if (Utility.chance(rand, .7)) {
						dirToGo = rc.getLocation().directionTo(base);
					} else if (Utility.chance(rand, .7) && enemiesWithinRange.length > 0) {
						dirToGo = rc.getLocation().directionTo(enemiesWithinRange[0].location).opposite();
					}
				prevDirection = Utility.tryToMove(rc, dirToGo, prevDirection);
				
			    } else if (enemiesSeen.length > 0) {
						//Move towards enemy
						RobotInfo closeEnemy = Utility.getClosest(enemiesSeen, rc.getLocation());
						prevDirection = Utility.tryToMove(rc, rc.getLocation().directionTo(closeEnemy.location), prevDirection);
				} else if (friendsSeen.length > 0) {
					
					RobotInfo[] closeFriends = rc.senseNearbyRobots(CLOSE_RANGE, myTeam); //Magic number
					RobotInfo[] medFriends = rc.senseNearbyRobots(MED_RANGE, myTeam); //More magic
					
					RobotInfo weakFriend = Utility.getWeakest(friendsSeen);
					int byteCode = Clock.getBytecodeNum();
					if (medFriends.length > MIN_SQUAD_NUM && weakFriend != null && weakFriend.weaponDelay > 1) {
						//Let's see if we have enough friends nearby
						//to assault enemies attacking team mates
						Direction dirToGo = rc.getLocation().directionTo(weakFriend.location);
						prevDirection = Utility.tryToMove(rc, dirToGo, prevDirection);
						rc.setIndicatorString(0, "First branch costs " + (Clock.getBytecodeNum() - byteCode));
						
					} else if (rc.getType().maxHealth / rc.getHealth() < RETREAT_HEALTH_PERCENT) {
						Direction dirToGo = Direction.NONE;
						if (Utility.chance(rand, .7)) {
							dirToGo = rc.getLocation().directionTo(base);
						} else if (Utility.chance(rand, .7) && enemiesWithinRange.length > 0) {
							dirToGo = rc.getLocation().directionTo(enemiesWithinRange[0].location).opposite();
						}
						prevDirection = Utility.tryToMove(rc, dirToGo, prevDirection);
						
				    } else if (closeFriends.length > CLOSE_TOO_MANY) {
						//Spread Apart if too many units adjacent
				    	//TODO May change to modify robots seen if byte code more efficient that way
				    	Direction dirOfType = null;
				    	RobotInfo[] nearFriends = rc.senseNearbyRobots(CLOSE_RANGE, myTeam);
				    	if (Utility.chance(rand, .1))
				    		dirOfType = Utility.getDirectionOfType(nearFriends, RobotType.SOLDIER, rc);
				    	Direction dirToGo = null;
				    	if (dirOfType != null) {
				    		dirToGo = dirOfType.opposite();
				    		// This will cause the guard to move away from soldiers
				    	} else {
				    		dirToGo = Utility.getRandomDirection(rand);
				    	}
				    	prevDirection = Utility.tryToMove(rc, dirToGo, prevDirection);
						rc.setIndicatorString(0, "Third branch costs " + (Clock.getBytecodeNum() - byteCode));
						
					} else if (closeFriends.length < CLOSE_TOO_FEW && Utility.chance(rand, .5)) {
						//Come together if med range is sparse
						RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc.getLocation());
						Direction dirToGo = null;
						if (Utility.chance(rand, .8)) {
							//Whether to clump or go home
							dirToGo = rc.getLocation().directionTo(closestFriend.location);
						} else {
							dirToGo =  rc.getLocation().directionTo(base);
						}
						prevDirection = Utility.tryToMove(rc, dirToGo, prevDirection);		
					} else if (medFriends.length > MED_TOO_MANY && Utility.chance(rand, .5)) {
						//Come together if med range is sparse
						Direction dirToGo = Utility.getRandomDirection(rand);
						prevDirection = Utility.tryToMove(rc, dirToGo, prevDirection);
						
					} else if (medFriends.length < MED_TOO_FEW && Utility.chance(rand, .5)) {
						//Come together if med range is sparse
						RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc.getLocation());
						Direction dirToGo = null;
						if (Utility.chance(rand, .8)) {
							//Whether to clump or go home
							dirToGo = rc.getLocation().directionTo(closestFriend.location);
						} else {
							dirToGo =  rc.getLocation().directionTo(base);
						}
						prevDirection = Utility.tryToMove(rc, dirToGo, prevDirection);	
					}
					//To Handle if they moved into a range where they can now shoot the enemy
					if (rc.isWeaponReady()) {
						enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), RobotType.GUARD.attackRadiusSquared);
						if(enemiesWithinRange.length > 0) { //We're in combat
							RobotInfo targetEnemy = Utility.getTarget(enemiesWithinRange, 0, rc.getLocation());
							if( targetEnemy != null) {
								rc.attackLocation(targetEnemy.location);
							}
						}
					}
				}
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield();
		}
	}
	//~~~~~~~~~~~~~~~~~END MAIN LOOP~~~~~~~~~~~~~~~~~~~~
	

	
	
}
