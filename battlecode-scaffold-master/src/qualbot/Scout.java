package qualbot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
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
	

	private final int sensorRadius = (int)Math.pow(sensorRadiusSquared, 0.5);
	private static final int globalBroadcastRange = 1000; //TODO make this nice
	
	//Objectives Information
	private final ArrayList<MapLocation> dens;
	private final ArrayList<MapLocation> predictedDens; //Guesses about den locations based on symmetry
	private final ArrayList<MapLocation> destroyedDens;
	private final HashMap<Integer, MapLocation> enemyArchons; //Enemy Archon IDs and last known locations
	private final ArrayList<MapLocation> neutrals;
	private byte[] fineBlocks = new byte[500];
	private HashMap<MapLocation, Integer> fineMap;
	private HashMap<MapLocation, Integer> roughMap;
	private final ArrayList<MapLocation> parts;
	
	//Robots Seen
	private RobotInfo[] enemiesInSight;
	private RobotInfo[] enemiesInRange;
	private RobotInfo[] friendsInSight;
	
	private boolean atObjective = false;
	private boolean findingOtherBounds = false;
	 
	//To be sorted
	private Direction cornerDirectionSearch = Direction.NONE;
	boolean checkAwayFromCenter;
	MapLocation trueCenter;
	
	//Scout-specific states
	public static final int SEARCHING = 90;
	
	public Scout(RobotController rc) {
		super(rc);
		this.dens = new ArrayList<MapLocation>();
		this.predictedDens = new ArrayList<MapLocation>();
		this.destroyedDens = new ArrayList<MapLocation>();
		this.enemyArchons = new HashMap<Integer, MapLocation>();
		this.neutrals = new ArrayList<MapLocation>();
		this.fineMap = new HashMap<MapLocation, Integer>(400);
		this.roughMap = new HashMap<MapLocation, Integer>(100);
		this.parts = new ArrayList<MapLocation>();

	}

	@Override
	public void run() {
		while(true) {
			try {
				handleMessages(); //TODO Possibly amortize to every other turn if bytecode gets too high
				myLocation = rc.getLocation();
				enemiesInSight = rc.senseHostileRobots(myLocation, -1);
				friendsInSight = rc.senseNearbyRobots(-1, myTeam);
				scanSurroundings();
				scanForBounds();
				rebroadcastKnowledge();

				RobotInfo nearestTurret = null;
				int minDist = 20; //Minimum nearestTurret distance
				for(RobotInfo friend : friendsInSight) {
					if(friend.type == RobotType.TURRET) {
						int distance = myLocation.distanceSquaredTo(friend.location);
						if(distance < minDist) {
							nearestTurret = friend;
							minDist = distance;
						}
					}
				}
				
				if(nearestTurret != null && enemiesInSight.length > 0) {
					RobotInfo targetEnemy = getAttackTarget(enemiesInSight, GameConstants.TURRET_MINIMUM_RANGE, nearestTurret.location);
					if(targetEnemy != null) {
						rc.broadcastMessageSignal(Comms.createHeader(Comms.TURRET_ATTACK_HERE), Comms.encodeLocation(targetEnemy.location), RobotType.SCOUT.sensorRadiusSquared);
						rc.setIndicatorString(1, "TARGET SIGHTED");
					} else rc.setIndicatorString(1, "");
				}
				rc.setIndicatorString(0,String.valueOf(state));
				
				if(state == IDLE) {
					if(dens.size() > 0) {//Siege a den if we can; the closest one to us
						int minDistance = Integer.MAX_VALUE;
						MapLocation denToSiege = dens.get(0);
						for(MapLocation den : dens) {
							int distance = den.distanceSquaredTo(myLocation);
							if(distance < minDistance) {
								minDistance = distance;
								denToSiege = den;
							}
						}
						state = SIEGING_DEN;
						objectiveFlag = denToSiege;
						rc.broadcastMessageSignal(Comms.createHeader(Comms.ATTACK_DEN), Comms.encodeLocation(denToSiege), globalBroadcastRange);
					} else if( !enemyArchons.isEmpty() && rc.getRobotCount() > 20) { //Attack the nearest enemy Archon
						int minDistance = Integer.MAX_VALUE;
						MapLocation archonToSiege = null;
						for(int archonID : enemyArchons.keySet()) {
							MapLocation loc = enemyArchons.get(archonID);
							int distance = myLocation.distanceSquaredTo(loc);
							if(distance < minDistance) {
								minDistance = distance;
								archonToSiege = loc;
							}
						}
						state = SIEGING_ENEMY;
						objectiveFlag = archonToSiege;
						rc.broadcastMessageSignal(Comms.createHeader(Comms.ATTACK_ENEMY), Comms.encodeLocation(archonToSiege), globalBroadcastRange);
					} else if(rc.getRoundNum() > 1500 && rc.getRoundNum()%10 == 0 && rc.getRobotCount() > 50) {
						state = SIEGING_ENEMY;
						objectiveFlag = enemyArchonStartPositions[0];
						rc.broadcastMessageSignal(Comms.createHeader(Comms.ATTACK_ENEMY), Comms.encodeLocation(objectiveFlag), globalBroadcastRange);
					} else {
						state = SEARCHING;
					}
				}
				
				//Don't waste a round changing state
				if( state == SIEGING_DEN) {
					gotoObjective(objectiveFlag, 30, 40, friendsInSight);
				} else if( state == SIEGING_ENEMY) {
					gotoObjective(objectiveFlag, 30, 40, friendsInSight);
					if(rc.getRoundNum()%200 == 0) state = IDLE; //TODO make this better
				} else if( state == SEARCHING) {
					boolean noCorners = !(minXFound || maxXFound || minYFound || maxYFound);
					if (noCorners) {
						Direction dirToGo = rc.getLocation().directionTo(mapCenter).opposite();
						if (dirToGo == Direction.EAST || dirToGo == Direction.NORTH) {
							dirToGo = Direction.NORTH_EAST;
						} else if (dirToGo == Direction.SOUTH || dirToGo == Direction.WEST) {
							dirToGo = Direction.SOUTH_WEST;
						}
						tryToMove(dirToGo);
					} else {
						if (mapSymmetry == Symmetry.XY || mapSymmetry == Symmetry.XY_STRONG) {
							trueCenter = mapCenter;
						}
					}
					if(!dens.isEmpty() || !enemyArchons.isEmpty() || rc.getRoundNum()%40 == 0) {
						state = IDLE;
					}
				}
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield(); //TODO: Move this? Exceptions waste a round this way
		}
	}

	protected void rebroadcastKnowledge(){
		//TODO: Put code here
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
							predictedDens.add(mirroredLocation(loc, mapSymmetry));
						}
						break;
					case Comms.PREDICTED_DEN_NOT_FOUND:
						predictedDens.remove(loc);
						break;
						
					case Comms.ATTACK_DEN:
						if(state == IDLE) {
							objectiveFlag = loc;
							state = SIEGING_DEN;
						}
						break;
					case Comms.DEN_DESTROYED:
						if(!dens.remove(loc)) //Remove if in dens
							predictedDens.remove(loc); //Remove from predicted if not in dens somehow
						destroyedDens.add(loc);
						if(state == SIEGING_DEN && objectiveFlag.equals(loc)) {
							state = IDLE;
						}
						break;
					case Comms.ATTACK_ENEMY:
						if(state == IDLE) {
							objectiveFlag = loc;
							state = SIEGING_ENEMY;
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
						break;
					case Comms.NEUTRAL_FOUND:
						neutrals.add(loc);
						break;
					case Comms.PARTS_FOUND:
						parts.add(loc);
						break;
					default:
						//
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
	 * Scans for Map Edges
	 * @throws GameActionException 
	 */
	private void scanForBounds() throws GameActionException {
		MapLocation cur = myLocation;
		if(!minXFound){
			for(int x = cur.x-sensorRadius+1; x <= cur.x; x++){
				MapLocation tile = new MapLocation(x, cur.y);
				if(!rc.onTheMap(tile)) {//Found a boundary
					minXFound = true;
					minX = x + 1;
				}
				else break;
			}
			if(minXFound) {
				rc.broadcastMessageSignal(Comms.createHeader(Comms.FOUND_MINX, minX), 0, globalBroadcastRange);
				if (mapSymmetry != Symmetry.Y) {
					maxXFound = true;
					maxX = mapCenter.x + (mapCenter.x - minX);
					rc.broadcastMessageSignal(Comms.createHeader(Comms.FOUND_MAXX, maxX), 0, globalBroadcastRange);
				}
			}
		}
		if(!maxXFound){
			for(int x = cur.x+sensorRadius-1; x >= cur.x; x--){
				MapLocation tile = new MapLocation(x, cur.y);
				if(!rc.onTheMap(tile)) {//Found a boundary
					maxXFound = true;
					maxX = x - 1;
				}
				else break;
			}
			if(maxXFound) {
				rc.broadcastMessageSignal(Comms.createHeader(Comms.FOUND_MAXX, maxX), 0, globalBroadcastRange);
				if (mapSymmetry != Symmetry.Y) {
					minXFound = true;
					minX = mapCenter.x + (mapCenter.x - maxX);
					rc.broadcastMessageSignal(Comms.createHeader(Comms.FOUND_MINX, minX), 0, globalBroadcastRange);
				}
			}
		}
		if(!minYFound){
			for(int y = cur.y-sensorRadius+1; y <= cur.y; y++){
				MapLocation tile = new MapLocation(cur.x, y);
				if(!rc.onTheMap(tile)) {//Found a boundary
					minYFound = true;
					minY = y + 1;
				}
				else break;
			}
			if(minYFound) {
				rc.broadcastMessageSignal(Comms.createHeader(Comms.FOUND_MINY, minY), 0, globalBroadcastRange);
				if (mapSymmetry != Symmetry.X) {
					maxYFound = true;
					maxY = mapCenter.y + (mapCenter.y - minY);
					rc.broadcastMessageSignal(Comms.createHeader(Comms.FOUND_MAXY, maxY), 0, globalBroadcastRange);
				}
			}
		}
		if(!maxYFound){
			for(int y = cur.y+sensorRadius-1; y >= cur.y; y--){
				MapLocation tile = new MapLocation(cur.x, y);
				if(!rc.onTheMap(tile)) {//Found a boundary
					maxYFound = true;
					maxY = y - 1;
				}
				else break;
			}
			if(maxYFound) {
				rc.broadcastMessageSignal(Comms.createHeader(Comms.FOUND_MAXY, maxY), 0, globalBroadcastRange);
				if (mapSymmetry != Symmetry.X) {
					minYFound = true;
					minY = mapCenter.y + (mapCenter.y - maxY);
					rc.broadcastMessageSignal(Comms.createHeader(Comms.FOUND_MINY, minY), 0, globalBroadcastRange);
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
				predictedDens.add(mirroredLocation(enemy.location, mapSymmetry));
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
		

		MapLocation[] partsNearby = rc.sensePartLocations(RobotType.ARCHON.sensorRadiusSquared);
		for(MapLocation pile : partsNearby) {
			if(!parts.contains(pile)) {
				parts.add(pile);
				double value = rc.senseParts(pile);
				if(value >= 20) {
					rc.broadcastMessageSignal(Comms.createHeader(Comms.PARTS_FOUND, (int)value), Comms.encodeLocation(pile), globalBroadcastRange);
					messages++;
				}
			}
			if(messages > 10) break;
		}
		
		ArrayList<MapLocation> removeDens = new ArrayList<>();
		for (MapLocation den : dens) {
			if(rc.canSense(den)) {
				RobotInfo robotAtDenLoc = rc.senseRobotAtLocation(den);
				if (robotAtDenLoc == null || robotAtDenLoc.type != RobotType.ZOMBIEDEN) {
					removeDens.add(den);
					destroyedDens.add(den);
					rc.broadcastMessageSignal(Comms.createHeader(Comms.DEN_DESTROYED), Comms.encodeLocation(den), globalBroadcastRange);
					if(state == SIEGING_DEN && objectiveFlag.equals(den)) {
						state = IDLE;
					}
				}
			}
		}
		for(MapLocation den : removeDens) dens.remove(den); //To avoid concurrent modification

		ArrayList<MapLocation> removePredictedDens = new ArrayList<>();
		for (MapLocation den : predictedDens) {
			int distanceToDen = myLocation.distanceSquaredTo(den);
			if (distanceToDen <= sensorRadiusSquared) {
				RobotInfo robotAtDenLoc = rc.senseRobotAtLocation(den);
				if (robotAtDenLoc == null || robotAtDenLoc.type != RobotType.ZOMBIEDEN) {
					removePredictedDens.add(den);
					rc.broadcastMessageSignal(Comms.createHeader(Comms.PREDICTED_DEN_NOT_FOUND), Comms.encodeLocation(den), globalBroadcastRange);
				}
			}
		}
		for(MapLocation den : removePredictedDens) predictedDens.remove(den); //To avoid concurrent modification
		
	}
}
