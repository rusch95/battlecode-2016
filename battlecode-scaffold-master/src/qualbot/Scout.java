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
	
	private static final int globalBroadcastRange = 10000; //TODO make this nice
	
	//Objectives Information
	private final ArrayList<MapLocation> dens;
	private final ArrayList<MapLocation> predictedDens; //Guesses about den locations based on symmetry
	private final HashMap<Integer, MapLocation> enemyArchons; //Enemy Archon IDs and last known locations
	private final ArrayList<MapLocation> neutrals;
	
	//Robots Seen
	private RobotInfo[] enemiesInSight;
	private RobotInfo[] enemiesInRange;
	private RobotInfo[] friendsInSight;
	
	private boolean atObjective = false;
	 
	//To be sorted
	private short[] mapLongitudesVisited = new short[280]; //Divide the map into 5 width lane
	
	//Scout-specific states
	public static final int SEARCHING = 90;
	
	public Scout(RobotController rc) {
		super(rc);
		this.dens = new ArrayList<MapLocation>();
		this.predictedDens = new ArrayList<MapLocation>();
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
					} else {
						state = SEARCHING;
					}
				}
				
				//Don't waste a round changing state
				if( state == SIEGING_DEN) {
					
				} else if( state == SIEGING_ENEMY) {
					
				} else if( state == SEARCHING) {
					
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
					case Comms.ATTACK_DEN:
						if(state == IDLE) {
							objectiveFlag = loc;
							state = SIEGING_DEN;
						}
						break;
					case Comms.DEN_DESTROYED:
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
				}
			}
			else { //Basic message
				
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
		
	}
	
	//Constants for gotoObjective
	private static final int[] forwardDirectionsToTry = {0, 1, -1};
	private static final int[] secondaryDirectionsToTry = {2, -2, 3, -3};
	private static final int[] allTheDirections = {0, 1, -1, 2, -2, 3, -3};
	
	private static final int tooFarAwayThreshold = 30;  //Don't consider friends distances greater than this from us
	private static final int closerToGoalThreshold = 6; //Go towards friend if closer to goal by this amount
	
	/**
	 * Goes to the location specified, and stays a certain distance away from it.
	 * @param flag objective location
	 * @param hysterisis margin required to initially be at the objective
	 * @param margin of distance that is satisfactory to be away from the objective
	 * @throws GameActionException
	 */
	private void gotoObjective(MapLocation flag, int hysterisis, int margin) throws GameActionException{
		if(rc.isCoreReady() && flag != null) {
			Direction dirToObjective =  myLocation.directionTo(flag);
			int distanceToObjective = myLocation.distanceSquaredTo(flag);
			if ( (distanceToObjective > hysterisis && !atObjective)	|| distanceToObjective > margin) {
				atObjective = distanceToObjective <= margin;
				//First let's see if we can move straight towards the objective
				for (int deltaD:forwardDirectionsToTry) {
					//TODO Could slightly optimize by choosing diagonal direction of most friends first
					Direction attemptDirection = Direction.values()[(dirToObjective.ordinal()+deltaD+8)%8];
					if(rc.canMove(attemptDirection)) {
						rc.move(attemptDirection);
						return;
					}
				}		
				//Move torwards some friend if they are closer to the goal than us
				RobotInfo friendCloserToGoal = null;
				for(RobotInfo robot:friendsInSight){
					//First let's find a friend that fits our profile
					int robotDistanceToGoal = robot.location.distanceSquaredTo(flag);
					if ((robotDistanceToGoal - closerToGoalThreshold) > distanceToObjective && myLocation.distanceSquaredTo(robot.location) < tooFarAwayThreshold) {			
						friendCloserToGoal = robot;
						break;
					}
				}
				//TODO Replace with a better movement towards friend, such as sideways in the friends direction
				if (friendCloserToGoal != null) {
					//And then let's move towards that friend
					Direction dirToFriend =  myLocation.directionTo(flag);
					for (int deltaD:forwardDirectionsToTry) {
						Direction attemptDirection = Direction.values()[(dirToFriend.ordinal()+deltaD+8)%8];
						if(rc.canMove(attemptDirection)) {
							rc.move(attemptDirection);
							return;
						}
					}	
				}
				//Finally, we try moving sideways or backwards
				for (int deltaD:secondaryDirectionsToTry) {
					Direction attemptDirection = Direction.values()[(dirToObjective.ordinal()+deltaD+8)%8];
					if(rc.canMove(attemptDirection)) {
						rc.move(attemptDirection);
						return;
					}
				}
			}
		}
		else atObjective = true;
	}
}
