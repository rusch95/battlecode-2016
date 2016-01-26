package qualbot;

import java.util.ArrayList;
import java.util.HashMap;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Archon extends Role {
	protected ArrayList<MapLocation> dens;
	private HashMap<MapLocation, Integer> parts;
	private ArrayList<MapLocation> neutrals;
	
	protected int objectiveMargin = 0;
	
	public Archon(RobotController rc) {
		super(rc);
		this.dens = new ArrayList<MapLocation>();
		this.parts = new HashMap<MapLocation, Integer>();
		this.neutrals = new ArrayList<MapLocation>();
	}

	@Override
	public void run() {
		while(true) {
			try {
				handleMessages();
				healAlly();
				myLocation = rc.getLocation();
				RobotInfo[] friendsInSight = rc.senseNearbyRobots(-1, myTeam);
				
				
				//TEST CODE PLEASE IGNORE
				if(rc.getTeamParts() > 130) {
					if(chance(0.85)) tryToBuild(RobotType.SOLDIER);
					else if(chance(0.5)) tryToBuild(RobotType.SCOUT);
					else tryToBuild(RobotType.TURRET);
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
					case Comms.PARTS_FOUND:
						parts.put(loc, aux);
						break;
					case Comms.NEUTRAL_FOUND:
						if(!neutrals.contains(loc)) neutrals.add(loc);
						break;
				}
			}
			else { //Basic message
				
			}
		}
	}
	
	/**
	 * Heals the weakest nearby friendly
	 * @throws GameActionException 
	 */
	public void healAlly() throws GameActionException {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(attackRadiusSquared, myTeam);
		RobotInfo weakestFriend = null;
		if (nearbyRobots.length > 0) {
        	double minHealth = Double.POSITIVE_INFINITY;
        	for (RobotInfo curBot : nearbyRobots){
        		if (curBot.health < minHealth && curBot.type != RobotType.ARCHON) {
        			minHealth = curBot.health;
        			weakestFriend = curBot;
        		}
        	}
		}
		if (weakestFriend != null) {
        	rc.repair(weakestFriend.location);
        }
	}
	
	private void tryToBuild(RobotType typeToBuild) throws GameActionException{
		if(rc.isCoreReady()) {
			for(int i = 0; i < 8; i++) {
				if(rc.canBuild(directions[i], typeToBuild)) { 
					rc.build(directions[i], typeToBuild);
					break;
				}
			}
		}
	}
}
