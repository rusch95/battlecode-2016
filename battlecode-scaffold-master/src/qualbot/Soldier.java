package qualbot;

import battlecode.common.Clock;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Signal;

public class Soldier extends Role {
	private boolean providingBackup = false; //Overrides the current state
	private int needsBackup;
	private MapLocation backupFlag;
	
	public Soldier(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		while(true) {
			try {
				if(providingBackup) { //supercedes the current state
					
				} else { //execute the current state
					
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

}
