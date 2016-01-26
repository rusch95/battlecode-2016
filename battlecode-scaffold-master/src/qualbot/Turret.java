package qualbot;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Turret extends Role {
	//Robots Seen
	private RobotInfo[] enemiesInSight;
	private RobotInfo[] friendsInSight;
	
	//Misc State fields
	private boolean providingBackup = false; //Overrides the current state
	private int needsBackup;
	private MapLocation backupFlag;
	private MapLocation objectiveFlag; //Current objective for the robot to move to
	private int objectiveMargin = 0; //This controls how close they get to an objective, such as one step away from dens
	private boolean atObjective=false; //True if the robot has entered the required margin for the objective
	private MapLocation targetEnemy;
	
	public Turret(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		while(true) {
			try {
				myLocation = rc.getLocation();
				enemiesInSight = rc.senseHostileRobots(myLocation, -1);
				targetEnemy = null;
				handleMessages();
				if (rc.getType() == RobotType.TURRET) {
					if(state == IDLE) {
						dealDamage();
					} else if (state == SIEGING_DEN) {
						dealDamage();
						if(enemiesInSight.length == 0 && !atObjective) {
							rc.pack();
						}
					} else if (state == SIEGING_ENEMY) {
						dealDamage();
						if(enemiesInSight.length == 0 && !atObjective) {
							rc.pack();
						}
					}
				} else if (rc.getType() == RobotType.TTM) {
					if (targetEnemy != null || enemiesInSight.length > 0) {
						//Unpack if we see enemies or our recon does
						rc.unpack();
					} else {
						gotoObjective(objectiveFlag, objectiveMargin, objectiveMargin+15, friendsInSight);
					}
				}
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield(); //TODO: Move this? Exceptions waste a round this way
		}
	}

	/**
	 * Attack a priority target if we can.
	 * @throws GameActionException 
	 */
	private void dealDamage() throws GameActionException {
		if(rc.isWeaponReady()) {
			RobotInfo nearbyTarget = null;
			if(enemiesInSight.length > 0) {
				nearbyTarget = getAttackTarget(enemiesInSight, minRange, myLocation);
			}
			if(nearbyTarget != null) {
				targetEnemy = nearbyTarget.location;
			}
			if( targetEnemy != null && rc.canAttackLocation(targetEnemy)) rc.attackLocation(targetEnemy);
		}
	}
	
	@Override
	protected void handleMessage(Signal message) {
		if(message.getTeam().equals(myTeam)) {
			int[] contents = message.getMessage();
			if(contents != null) { //Advanced message
				int code = Comms.getMessageCode(contents[0]);
				int aux = Comms.getAux(contents[0]);
				MapLocation loc = Comms.decodeLocation(contents[1]);
				switch (code){
					case Comms.FOUND_MINX:
						if(!minXFound) {
							minX = aux;
							minXFound = true;
						}
						break;
					case Comms.FOUND_MAXX:
						if(!maxXFound) {
							maxX = aux;
							maxXFound = true;
						}
						break;
					case Comms.FOUND_MINY:
						if(!minYFound) {
							minY = aux;
							minYFound = true;
						}
						break;
					case Comms.FOUND_MAXY:
						if(!maxYFound) {
							maxY = aux;
							maxYFound = true;
						}
						break;
					case Comms.TURRET_ATTACK_HERE:
						if(rc.canAttackLocation(loc)) targetEnemy = loc;
						break;
				}
			}
			else { //Basic message
				
			}
		}
	}
}
