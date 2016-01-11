package FSMbot_1;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

public class Turret implements Role {
	private final RobotController rc;
	private final Random rand;
    private final Team myTeam;
    private final Team otherTeam;
    
	public Turret(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
	}
	
	@Override
	public void run() {
		while(true){
			try {
				RobotInfo[] enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), 100000); //Turret range > sight
				if(enemiesWithinRange.length > 0) { //We're in combat
					RobotInfo weakestEnemy = Utility.getWeakestForTurret(enemiesWithinRange, rc.getLocation());
					if(rc.isWeaponReady() && weakestEnemy != null) {
						rc.attackLocation(weakestEnemy.location);
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
