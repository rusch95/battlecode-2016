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
					RobotInfo targetEnemy = Utility.getTarget(enemiesWithinRange, 0, rc);
					if(rc.isWeaponReady() && targetEnemy != null) {
						rc.attackLocation(targetEnemy.location);
					} 
				} else if (enemiesSeen.length > 0) {
						//Move towards enemy
						RobotInfo closeEnemy = Utility.getClosest(enemiesSeen, rc);
						Utility.tryToMove(rc, rc.getLocation().directionTo(closeEnemy.location));
				} else if (friendsSeen.length > 0) {
					
					int closeFriendNum = Utility.getNumOfFriendsWithinRange(friendsSeen, rc, 0, 5); //Magic number
					int moreFriendNum = Utility.getNumOfFriendsWithinRange(friendsSeen, rc, 0, 25); //More magic
					//More Magic
					int reallyCloseTooMany = 3;
					int tooFewNearby = 3;
					int minSquadNum = 0;
					
					RobotInfo weakFriend = Utility.getWeakest(friendsSeen);
					
					if (moreFriendNum > minSquadNum && weakFriend != null && weakFriend.weaponDelay > 1) {
						//Let's see if we have enough friends nearby
						//to assault enemies attacking team mates
						Direction dirToGo = rc.getLocation().directionTo(weakFriend.location);
						Utility.tryToMove(rc, dirToGo);
					} else if (moreFriendNum > 0 && weakFriend!= null && weakFriend.weaponDelay > 1) {
						//Let's go regroup
						RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc); 
						Direction dirToGo = rc.getLocation().directionTo(closestFriend.location);
						Utility.tryToMove(rc, dirToGo);	
				    } else if (closeFriendNum > reallyCloseTooMany) {
						//Spread Apart if too many units adjacent
				    	//TODO May change to modify robots seen if byte code more efficient that way
				    	RobotInfo[] adjFriends = rc.senseNearbyRobots(2, myTeam);
				    	RobotInfo botOfType = Utility.getBotOfType(adjFriends, RobotType.SOLDIER, rand, rc);
				    	Direction dirToGo = Direction.NONE;
				    	if (botOfType != null) {
				    		dirToGo = botOfType.location.directionTo(rc.getLocation());
				    		// This will cause the guard to move away from soldiers
				    	} else {
				    		dirToGo = Utility.getRandomDirection(rand);
				    	}
						Utility.tryToMove(rc, dirToGo);
					} else if (tooFewNearby > closeFriendNum) {
						//Come together if med range is sparse
						RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc);
						Direction dirToGo = rc.getLocation().directionTo(closestFriend.location);
						Utility.tryToMove(rc, dirToGo);		
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
