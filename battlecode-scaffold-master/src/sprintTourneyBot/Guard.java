package sprintTourneyBot;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Guard implements Role {
	private final RobotController rc;
	private final Random rand;
    private final Team myTeam;
    private final Team otherTeam;
    
    //Magic Numbers
    private final int closeRange = 5;
    private final int medRange = 15;
    private final int farRange = 25;
    private final int maxRange = -1;
	private final int closeTooManyThreshold = 2;
	private final int closeTooFewThreshold = 2;
	private final int medTooManyThreshold = 0;
	private final int medTooFewThreshold = 4;
    
	public Guard(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
	}
	
	@Override
	public void run() {
		while(true){
			try {
				RobotInfo[] enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), RobotType.GUARD.attackRadiusSquared);
				RobotInfo[] enemiesSeen = rc.senseHostileRobots(rc.getLocation(), -1);
				RobotInfo[] friendsSeen = rc.senseNearbyRobots(-1, myTeam);
				if(enemiesWithinRange.length > 0) { //We're in combat
					RobotInfo targetEnemy = Utility.getTarget(enemiesWithinRange, 0, rc.getLocation());
					if(rc.isWeaponReady() && targetEnemy != null) {
						rc.attackLocation(targetEnemy.location);
					} 
				} else if (enemiesSeen.length > 0) {
						//Move towards enemy
						RobotInfo closeEnemy = Utility.getClosest(enemiesSeen, rc.getLocation());
						Utility.tryToMove(rc, rc.getLocation().directionTo(closeEnemy.location));
				} else if (friendsSeen.length > 0) {
					
					RobotInfo[] closeFriends = rc.senseNearbyRobots(closeRange, myTeam); //Magic number
					RobotInfo[] moreFriends = rc.senseNearbyRobots(medRange, myTeam); //More magic
					
					RobotInfo weakFriend = Utility.getWeakest(friendsSeen);
					int byteCode = Clock.getBytecodeNum();
					if (moreFriends.length > minSquadNum && weakFriend != null && weakFriend.weaponDelay > 1) {
						//Let's see if we have enough friends nearby
						//to assault enemies attacking team mates
						Direction dirToGo = rc.getLocation().directionTo(weakFriend.location);
						Utility.tryToMove(rc, dirToGo);
						rc.setIndicatorString(0, "First branch costs " + (Clock.getBytecodeNum() - byteCode));
					} else if (moreFriends.length > 0 && weakFriend!= null && weakFriend.weaponDelay > 1) {
						//Let's go regroup
						RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc.getLocation()); 
						Direction dirToGo = rc.getLocation().directionTo(closestFriend.location);
						Utility.tryToMove(rc, dirToGo);	
						rc.setIndicatorString(0, "Second branch costs " + (Clock.getBytecodeNum() - byteCode));
				    } else if (closeFriends.length > reallyCloseTooMany) {
						//Spread Apart if too many units adjacent
				    	//TODO May change to modify robots seen if byte code more efficient that way
				    	Direction dirOfType = null;
				    	RobotInfo[] nearFriends = rc.senseNearbyRobots(5, myTeam);
				    	if (Utility.chance(rand, .1))
				    		dirOfType = Utility.getDirectionOfType(nearFriends, RobotType.SOLDIER, rc);
				    	Direction dirToGo = null;
				    	if (dirOfType != null) {
				    		dirToGo = dirOfType.opposite();
				    		// This will cause the guard to move away from soldiers
				    	} else {
				    		dirToGo = Utility.getRandomDirection(rand);
				    	}
						Utility.tryToMove(rc, dirToGo);
						rc.setIndicatorString(0, "Third branch costs " + (Clock.getBytecodeNum() - byteCode));
					} else if (tooFewNearby > closeFriends.length) {
						//Come together if med range is sparse
						RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc.getLocation());
						Direction dirToGo = rc.getLocation().directionTo(closestFriend.location);
						Utility.tryToMove(rc, dirToGo);		
						rc.setIndicatorString(0, "Last branch costs " + (Clock.getBytecodeNum() - byteCode));
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
