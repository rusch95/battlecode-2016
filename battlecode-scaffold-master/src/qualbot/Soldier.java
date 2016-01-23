package qualbot;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Signal;

public class Soldier extends Role {
	private boolean providingBackup = false; //Overrides the current state
	private int needsBackup;
	private MapLocation backupFlag;
	
	private RobotInfo[] enemiesInSight;
	private RobotInfo[] enemiesInRange;
	
	public Soldier(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		while(true) {
			try {
				handleMessages();
				myLocation = rc.getLocation();
				enemiesInSight = rc.senseHostileRobots(myLocation, -1);
				enemiesInRange = rc.senseHostileRobots(myLocation, attackRadiusSquared);
				if(providingBackup) { //supercedes the current state
					if(enemiesInRange.length > 0) {
						kite();
					} else {
						//move towards backupFlag
					}
				} else { //execute the current state
					if(enemiesInRange.length > 0) {
						kite();
					} else {
						//go towards objective
					}
				}
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield(); //TODO: Move this? Exceptions waste a round this way
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
					case Comms.ATTACK_DEN:
						if(state == IDLE) {
							targetFlag = loc;
							state = SEIGING_DEN;
						}
						break;
					case Comms.DEN_DESTROYED:
						if(state == SEIGING_DEN && targetFlag.equals(loc)) {
							state = IDLE;
						}
						break;
					case Comms.ATTACK_ENEMY:
						if(state == IDLE) {
							targetFlag = loc;
							state = SEIGING_ENEMY;
						}
						break;
					case Comms.NEED_BACKUP:
						providingBackup = true;
						backupFlag = loc;
						needsBackup = message.getID();
						break;
					case Comms.NO_LONGER_NEED_BACKUP:
						if(providingBackup && message.getID() == needsBackup) {
							providingBackup = false;
						}
						break;
				}
			}
			else { //Basic message
				
			}
		}
	}

	/**
	 * Attack a target, and then move to avoid enemies.
	 * @throws GameActionException 
	 */
	private void kite() throws GameActionException{
		if(rc.isWeaponReady()) {
			RobotInfo target = getAttackTarget(enemiesInRange, minRange, myLocation);
			if(rc.canAttackLocation(target.location)) {
				rc.attackLocation(target.location);
			} else {
				rc.setIndicatorString(0, "FAILED TO ATTACK");
			}
		}
		dodgeEnemies();
	}
	
}
