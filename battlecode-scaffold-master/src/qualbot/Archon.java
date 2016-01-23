package qualbot;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Archon extends Role {
	protected ArrayList<MapLocation> dens;
	
	
	public Archon(RobotController rc) {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		while(true) {
			try {
				handleMessages();
				myLocation = rc.getLocation();
				
				//TEST CODE PLEASE IGNORE
				tryToBuild(RobotType.SOLDIER);
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
					case Comms.DEN_FOUND:
						if(!dens.contains(loc)) dens.add(loc);
						break;
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

	private void tryToBuild(RobotType typeToBuild) throws GameActionException{
		if(rc.isCoreReady()) {
			for(int i = 0; i < 8; i++) {
				if(rc.canBuild(directions[i], typeToBuild)) rc.build(directions[i], typeToBuild);
			}
		}
	}
}
