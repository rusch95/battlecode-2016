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
	private RobotInfo[] enemiesInRange;
	private RobotInfo[] friendsInSight;
	
	//Misc State fields
	private boolean providingBackup = false; //Overrides the current state
	private int needsBackup;
	private MapLocation backupFlag;
	private MapLocation objectiveFlag; //Current objective for the robot to move to
	private int objectiveMargin = 0; //This controls how close they get to an objective, such as one step away from dens
	private boolean atObjective=false; //True if the robot has entered the required margin for the objective
	private RobotInfo targetEnemy;
	
	public Turret(RobotController rc) {
		super(rc);
		objectiveFlag = mapCenter;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		while(true) {
			try {
				myLocation = rc.getLocation();
				enemiesInSight = rc.senseHostileRobots(myLocation, -1);
				if (rc.getType() == RobotType.TURRET) {
					handleMessages();
					if (enemiesInSight.length == 0)
						rc.pack();
				} else if (rc.getType() == RobotType.TTM) {
					handleMessages();
					if (enemiesInSight.length > 0) {
						//Unpack if we see enemies
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
				}
			}
			else { //Basic message
				
			}
		}
	}
}
