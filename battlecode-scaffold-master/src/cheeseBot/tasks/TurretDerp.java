package cheeseBot.tasks;

import java.util.Random;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import cheeseBot.Task;

public class TurretDerp implements Task {
    private int myAttackRange;
    private Team myTeam;
    private Team enemyTeam;
    private RobotController rc;
    private Random rand;
	
	public TurretDerp(RobotController rc) {
		this.rc = rc;
    	this.myTeam = rc.getTeam();
    	this.enemyTeam = myTeam.opponent();
    	this.myAttackRange = rc.getType().attackRadiusSquared;
    	this.rand = new Random(rc.getID());
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
	
	@Override
	public int run() throws GameActionException {
		// If this robot type can attack, check for enemies within range and attack one
		int fate = rand.nextInt(1000);
		
		if (rc.getType() == RobotType.TTM) {
			return 1;
		}
		
        if (rc.isWeaponReady()) {
            RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, enemyTeam);
            RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
            if (enemiesWithinRange.length > 0) {
                for (RobotInfo enemy : enemiesWithinRange) {
                    // Check whether the enemy is in a valid attack range (turrets have a minimum range)
                    if (rc.canAttackLocation(enemy.location)) {
                        rc.attackLocation(enemy.location);
                        break;
                    }
                }
            } else if (zombiesWithinRange.length > 0) {
                for (RobotInfo zombie : zombiesWithinRange) {
                    if (rc.canAttackLocation(zombie.location)) {
                        rc.attackLocation(zombie.location);
                        break;
                    }
                }   
            } else if (fate < 333) {
            	RobotInfo[] neighbors = rc.senseNearbyRobots(5, rc.getTeam());
            	if (getNumberOfBotOfType(neighbors, RobotType.TURRET) > 2) {
            		if (rc.getType() == RobotType.TURRET)
            			rc.pack();
            			return 0;
            	}
            	
            }
        }
        return 0;
	}

}
