package role_task_abstractionbot.tasks;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import role_task_abstractionbot.Task;

public class TurretDerp implements Task {
    private int myAttackRange;
    private Team myTeam;
    private Team enemyTeam;
    private RobotController rc;
	
	public TurretDerp(RobotController rc) {
		this.rc = rc;
    	this.myTeam = rc.getTeam();
    	this.enemyTeam = myTeam.opponent();
    	this.myAttackRange = rc.getType().attackRadiusSquared;
	}

	@Override
	public int run() throws GameActionException {
		// If this robot type can attack, check for enemies within range and attack one
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
            }
        }
        return 0;
	}

}
