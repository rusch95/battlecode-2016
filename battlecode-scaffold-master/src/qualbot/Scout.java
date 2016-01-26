package qualbot;

import java.util.ArrayList;
import java.util.HashMap;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;

public class Scout extends Role {
	private boolean providingBackup = false; //Overrides the current state
	private int needsBackup;
	private MapLocation backupFlag;
	private MapLocation objectiveLocation; //Current objective for the robot to move to
	
	private static final int globalBroadcastRange = 10000; //TODO make this nice
	
	//Objectives Information
	private final ArrayList<MapLocation> dens;
	private final ArrayList<MapLocation> predictedDens; //Guesses about den locations based on symmetry
	private final ArrayList<MapLocation> destroyedDens;
	private final HashMap<Integer, MapLocation> enemyArchons; //Enemy Archon IDs and last known locations
	private final ArrayList<MapLocation> neutrals;
	
	//Robots Seen
	private RobotInfo[] enemiesInSight;
	private RobotInfo[] enemiesInRange;
	private RobotInfo[] friendsInSight;
	
	private boolean atObjective = false;
	private boolean explore = false;
	 
	//To be sorted
	private short[] mapLongitudesVisited = new short[280]; //Divide the map into 5 width lane
	
	public Scout(RobotController rc) {
		super(rc);
		this.dens = new ArrayList<MapLocation>();
		this.predictedDens = new ArrayList<MapLocation>();
		this.destroyedDens = new ArrayList<MapLocation>();
		this.enemyArchons = new HashMap<Integer, MapLocation>();
		this.neutrals = new ArrayList<MapLocation>();
	}

	@Override
	public void run() {
		while(true) {
			try {
				handleMessages(); //TODO Possibly amortize to every other turn if bytecode gets too high
				myLocation = rc.getLocation();
				enemiesInSight = rc.senseHostileRobots(myLocation, -1);
				enemiesInRange = rc.senseHostileRobots(myLocation, attackRadiusSquared);
				friendsInSight = rc.senseNearbyRobots(-1, myTeam);
				scanSurroundings();
				if (explore) {
					
				}
				
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield(); //TODO: Move this? Exceptions waste a round this way
		}
	}

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
					case Comms.DEN_FOUND:
						if (!dens.contains(loc)) {
							dens.add(loc);
							predictedDens.remove(loc);
						}
						break;
					case Comms.PREDICTED_DEN_NOT_FOUND:
						predictedDens.remove(loc);
						break;
						
					case Comms.ATTACK_DEN:
						if(state == IDLE) {
							targetFlag = loc;
							state = SEIGING_DEN;
						}
						break;
					case Comms.DEN_DESTROYED:
						if(!dens.remove(loc)); //Remove if in dens
							predictedDens.remove(loc); //Remove from predicted if not in dens somehow
						destroyedDens.add(loc);
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
					case Comms.ENEMY_ARCHON_SIGHTED:
						enemyArchons.put(aux, loc);
					default:
						System.out.println("Code not implemented: " + code);
				}
			}
			else { //Basic message
			}
		} else { //Enemy message //TODO Use this for army
			if (enemyArchons.containsKey(message.getID())) {
				enemyArchons.put(message.getID(), message.getLocation());
				try {
					rc.broadcastMessageSignal(Comms.createHeader(Comms.ENEMY_ARCHON_SIGHTED, message.getID()), Comms.encodeLocation(message.getLocation()), globalBroadcastRange);
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Check surrounding map for things worth reporting
	 * @throws GameActionException 
	 */
	private void scanSurroundings() throws GameActionException {
		double messages = 0;
		
		for(RobotInfo enemy : enemiesInSight) { //Look for dens/archons
			if(enemy.type.equals(RobotType.ZOMBIEDEN) && !dens.contains(enemy.location)) {
				dens.add(enemy.location);
				predictedDens.remove(enemy.location);
				rc.broadcastMessageSignal(Comms.createHeader(Comms.DEN_FOUND), Comms.encodeLocation(enemy.location), globalBroadcastRange);
				messages++;
			} else if(enemy.type.equals(RobotType.ARCHON)) {
				enemyArchons.put(enemy.ID, enemy.location);
				rc.broadcastMessageSignal(Comms.createHeader(Comms.ENEMY_ARCHON_SIGHTED, enemy.ID), Comms.encodeLocation(enemy.location), globalBroadcastRange);
				messages++;
			}
		}
		
		for(RobotInfo neutral : rc.senseNearbyRobots(-1, Team.NEUTRAL)) {
			if(!neutrals.contains(neutral.location)) {
				neutrals.add(neutral.location);
				rc.broadcastMessageSignal(Comms.createHeader(Comms.NEUTRAL_FOUND), Comms.encodeLocation(neutral.location), globalBroadcastRange);
				messages++;
				if(messages > 10) break;
			}
		}
		
		MapLocation[] parts = rc.sensePartLocations(RobotType.ARCHON.sensorRadiusSquared);
		for(MapLocation pile : parts) {
			double value = rc.senseParts(pile);
			if(value >= 20) {
				rc.broadcastMessageSignal(Comms.createHeader(Comms.PARTS_FOUND, (int)value), Comms.encodeLocation(pile), globalBroadcastRange);
				messages++;
			}
			if(messages > 10) break;
		}
		
		if (!rc.isCoreReady()) { //Amortize
			for (MapLocation den:dens) {
				int distanceToDen = myLocation.distanceSquaredTo(den);
				if (distanceToDen <= sensorRadiusSquared) {
					RobotInfo robotAtDenLoc = rc.senseRobotAtLocation(den);
					if (robotAtDenLoc == null || robotAtDenLoc.type != RobotType.ZOMBIEDEN) {
						dens.remove(den);
						destroyedDens.add(den);
						rc.broadcastMessageSignal(Comms.createHeader(Comms.DEN_DESTROYED), Comms.encodeLocation(den), globalBroadcastRange);
					}
				}
			}
		} else { 
			for (MapLocation den:predictedDens) {
				int distanceToDen = myLocation.distanceSquaredTo(den);
				if (distanceToDen <= sensorRadiusSquared) {
					RobotInfo robotAtDenLoc = rc.senseRobotAtLocation(den);
					if (robotAtDenLoc == null || robotAtDenLoc.type != RobotType.ZOMBIEDEN) {
						predictedDens.remove(den);
						rc.broadcastMessageSignal(Comms.createHeader(Comms.PREDICTED_DEN_NOT_FOUND), Comms.encodeLocation(den), globalBroadcastRange);
					}
				}
			}
		}
	}
}
