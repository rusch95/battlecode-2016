package sprintTourneyBot;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import sprintTourneyBot.Utility;

public class Soldier implements Role {
	private final RobotController rc;
	private final Random rand;
    private final Team myTeam;
    private final Team otherTeam;
    
	public Soldier(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
	}
	
	@Override
	public void run() {
		while(true){
			try {
				RobotInfo[] enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), RobotType.SOLDIER.attackRadiusSquared);
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
					
					RobotInfo[] closeFriends = rc.senseNearbyRobots(5, myTeam); //Magic number
					RobotInfo[] moreFriends = rc.senseNearbyRobots(15, myTeam); //More magic
					//More Magic
					int reallyCloseTooMany = 5;
					int tooFewNearby = 3;
					int minSquadNum = 0;
					
					RobotInfo weakFriend = Utility.getWeakest(friendsSeen);
					
					if (moreFriends.length > minSquadNum && weakFriend != null && weakFriend.weaponDelay > 1) {
						//Let's see if we have enough friends nearby
						//to assault enemies attacking team mates
						Direction dirToGo = rc.getLocation().directionTo(weakFriend.location);
						Utility.tryToMove(rc, dirToGo);
					} else if (moreFriends.length > 0 && weakFriend!= null && weakFriend.weaponDelay > 1) {
						//Let's go regroup
						RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc.getLocation()); 
						Direction dirToGo = rc.getLocation().directionTo(closestFriend.location);
						Utility.tryToMove(rc, dirToGo);	
				    } else if (closeFriends.length > reallyCloseTooMany) {
						//Spread Apart if too many units adjacent
						Direction dirToGo = Utility.getRandomDirection(rand);
						Utility.tryToMove(rc, dirToGo);
					} else if (tooFewNearby > closeFriends.length && Utility.chance(rand, .5)) {
						//Come together if med range is sparse
						RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc.getLocation());
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
