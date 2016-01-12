package sprintTourneyBot;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Soldier implements Role {
	private final RobotController rc;
	private final Random rand;
    private final Team myTeam;
    private final Team otherTeam;
    private final MapLocation startLocation;
    
    //Magic Numbers
    private final int CLOSE_RANGE = 5;
    private final int MED_RANGE = 17;
    private final int FAR_RANGE = 25;
    private final int MAX_RANGE = -1;
	private final int CLOSE_TOO_MANY = 4;
	private final int CLOSE_TOO_FEW = 4;
	private final int MED_TOO_MANY = 10;
	private final int MED_TOO_FEW = 5;
	private final int FAR_TOO_MANY = 999;
	private final int FAR_TOO_FEW = 0;
	private final int MIN_SQUAD_NUM = 1;
    
	public Soldier(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
		this.startLocation = rc.getLocation();
	}
	
	@Override
	public void run() {
		while(true){
			try {
				RobotInfo[] enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), RobotType.SOLDIER.attackRadiusSquared);
				RobotInfo[] enemiesSeen = rc.senseHostileRobots(rc.getLocation(), MAX_RANGE);
				RobotInfo[] friendsSeen = rc.senseNearbyRobots(MAX_RANGE, myTeam);
				if(enemiesWithinRange.length > 0) { //We're in combat
					RobotInfo targetEnemy = Utility.getTarget(enemiesWithinRange, 0, rc.getLocation());
					if(rc.isWeaponReady() && targetEnemy != null) {
						rc.attackLocation(targetEnemy.location);
					}
				}
				if (enemiesSeen.length > 0) {
					//Move towards enemy
					RobotInfo closeEnemy = Utility.getClosest(enemiesSeen, rc.getLocation());
					Utility.tryToMove(rc, rc.getLocation().directionTo(closeEnemy.location));
				} else if (friendsSeen.length > 0) {
					
					RobotInfo[] closeFriends = rc.senseNearbyRobots(CLOSE_RANGE, myTeam); //Magic number
					RobotInfo[] medFriends = rc.senseNearbyRobots(MED_RANGE, myTeam); //More magic
					
					RobotInfo weakFriend = Utility.getWeakest(friendsSeen);
					
					if (medFriends.length > MIN_SQUAD_NUM && weakFriend != null  && weakFriend.weaponDelay > 1 && (weakFriend.type != RobotType.ARCHON || Utility.chance(rand, .6))) {
						//Let's see if we have enough friends nearby
						//to assault enemies attacking team mates
						Direction dirToGo = rc.getLocation().directionTo(weakFriend.location);
						Utility.tryToMove(rc, dirToGo);
				    } else if (closeFriends.length > CLOSE_TOO_MANY && Utility.chance(rand, .5)) {
						//Spread Apart if too many units adjacent
						Direction dirToGo = Utility.getRandomDirection(rand);
						Utility.tryToMove(rc, dirToGo);
					} else if (closeFriends.length < CLOSE_TOO_FEW && Utility.chance(rand, .5)) {
						//Come together if med range is sparse
						RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc.getLocation());
						Direction dirToGo = null;
						if (Utility.chance(rand, .8)) {
							//Whether to clump or go home
							dirToGo = rc.getLocation().directionTo(closestFriend.location);
						} else {
							dirToGo =  rc.getLocation().directionTo(startLocation);
						}
						Utility.tryToMove(rc, dirToGo);	
					} else if (medFriends.length > MED_TOO_MANY && Utility.chance(rand, .5)) {
						//Come together if med range is sparse
						Direction dirToGo = Utility.getRandomDirection(rand);
						Utility.tryToMove(rc, dirToGo);		
					} else if (medFriends.length < MED_TOO_FEW && Utility.chance(rand, .5)) {
						//Come together if med range is sparse
						RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc.getLocation());
						Direction dirToGo = null;
						if (Utility.chance(rand, .8)) {
							//Whether to clump or go home
							dirToGo = rc.getLocation().directionTo(closestFriend.location);
						} else {
							dirToGo =  rc.getLocation().directionTo(startLocation);
						}
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
