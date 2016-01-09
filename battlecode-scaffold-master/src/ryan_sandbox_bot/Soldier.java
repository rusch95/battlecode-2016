package ryan_sandbox_bot;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

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
				if(enemiesWithinRange.length > 0) { //We're in combat
					RobotInfo weakestEnemy = Utility.getWeakest(enemiesWithinRange);
					if(rc.isWeaponReady() && weakestEnemy != null) {
						rc.attackLocation(weakestEnemy.location);
					}
				} else {
					RobotInfo[] friendsNearby = rc.senseNearbyRobots(10000, myTeam);
					RobotInfo weakestFriend = Utility.getWeakest(friendsNearby);
					if(weakestFriend.weaponDelay > 1) { //Injured friend nearby and fighting
						if(rc.isCoreReady()) {
							Utility.tryToMove(rc, rc.getLocation().directionTo(weakestFriend.location));
						}
					}
				}
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, myTeam);
				if(nearbyFriends.length > 3) {
					//Try to move
					if(rc.isCoreReady()) {
						Utility.tryToMove(rc, Utility.getRandomDirection(rand));
					}
				}
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield();
		}
		
	}
	
}
