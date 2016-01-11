package FSMbot_1;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;

public class Archon implements Role {
	private RobotController rc;
	private Random rand;
	private final Team myTeam;
	private String state;
	private String nextState;
	private MapLocation target;
    
	//Surroundings information
	private int nearbyTurrets;
	private int nearbyPawns;
	private Direction retreatDirection;
	
	//Map information
	private ArrayList<MapLocation> knownDens = new ArrayList<MapLocation>();
	
	//Constants
	private static final int MIGRATION_TARGET_THRESHOLD = 4;
	private static final int DEN_SEIGE_THRESHOLD = 8;
	private static final int DEN_TOO_CLOSE_THRESHOLD = 20;
	private static final int PARTS_TARGET_THRESHOLD = 0;
	private static final int PANIC_THRESHOLD = 8;
	private static final int SQUAD_RADIUS = 7;
	private static final int MIN_SURROUNDING_TURRETS = 1;
	
	//Message codes
	private static final int APPROACHING_DEN = 70;
	
	
	/*
	 * Current states:
	 * TURTLE:
	 * 		build turrets and other units to fortify a position.
	 * APPROACH_DEN:
	 * 		Go towards a known den with intent to seige.
	 * SEIGE_DEN:
	 * 		Set up a seige upon a den.
	 * APPROACH_PARTS:
	 * 		Go towards a known part location to collect.
	 * MIGRATE:
	 * 		Go to a safer location to fortify
	 * RETREAT_ZOMBIES:
	 * 		Avoid a zombie horde.
	 * PANIC_ZOMBIES:
	 * 		Build a quick zombie defense.
	 * RETREAT_OPPONENT:
	 * 		Avoid opposing army
	 * PANIC_OPPONENT:
	 * 		Build a quick opponent defense
	 */
	
	public Archon(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.state = "TURTLE";
	}
	
	@Override
	public void run() {
		while(true){
			try {
				int fate = rand.nextInt(1000);
				rc.setIndicatorString(0, "My current state is: " + state);
				
				Signal[] messages = rc.emptySignalQueue();
				handleMessages(messages);
				scanArea();
				
				if(state.equals("MIGRATE")) {
					if(rc.getLocation().distanceSquaredTo(target) <= MIGRATION_TARGET_THRESHOLD) {
						nextState = "TURTLE";
					}
					Utility.tryToMove(rc, rc.getLocation().directionTo(target));
				}
				else if(state.equals("APPROACH_DEN")) {
					rc.broadcastMessageSignal(APPROACHING_DEN, Utility.encodeLocation(target), SQUAD_RADIUS);
					if(rc.getLocation().distanceSquaredTo(target) <= DEN_SEIGE_THRESHOLD) {
						nextState = "SEIGE_DEN";
					}
					Utility.tryToMove(rc, rc.getLocation().directionTo(target));
				}
				else if(state.equals("SEIGE_DEN")) {
					//TODO
				}
				else if(state.equals("APPROACH_PARTS")) {
					if(rc.getLocation().distanceSquaredTo(target) <= PARTS_TARGET_THRESHOLD) {
						nextState = "TURTLE";
					}
					Utility.tryToMove(rc, rc.getLocation().directionTo(target));
				}
				else if(state.equals("TURTLE")) {
					if(fate%2 == 1 && rc.getTeamParts() > RobotType.TURRET.partCost){
						if(nearbyTurrets <= MIN_SURROUNDING_TURRETS) {
							tryToBuild(RobotType.TURRET);
						}
						else {
							tryToBuild(RobotType.SOLDIER);
						}
					}
					nextState = "TURTLE";
				}
				else if(state.equals("RETREAT_ZOMBIES")) {
					Utility.tryToMove(rc, retreatDirection);
					nextState = "PANIC_ZOMBIES";
				}
				else if(state.equals("RETREAT_OPPONENT")) {
					Utility.tryToMove(rc, retreatDirection);
					nextState = "PANIC_OPPONENT";
				}
				else if(state.equals("PANIC_ZOMBIES")) {
					nextState = "TURTLE"; //TODO change this
				}
				else if(state.equals("PANIC_OPPONENT")) {
					nextState = "TURTLE"; //TODO change this
				}
				
				
				state = nextState;
				
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield();
		}
		
	}
	//~~~~~~~~~~~~~~~~END OF MAIN LOOP~~~~~~~~~~~~~~~~
	
	/**
	 * Scans the sight range of the Archon for stimuli
	 */
	private void scanArea() {
		//MapLocation[] surroundings = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), RobotType.ARCHON.sensorRadiusSquared);
		RobotInfo[] nearbyHostiles = rc.senseHostileRobots(rc.getLocation(), PANIC_THRESHOLD); //TODO change this to full range for smarter actions?
		RobotInfo[] nearbyFriendlies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, myTeam);
		nearbyTurrets = 0;
		nearbyPawns = 0;
		
		for(RobotInfo hostile : nearbyHostiles) {
			if(hostile.location.distanceSquaredTo(rc.getLocation()) <= PANIC_THRESHOLD) {
				state = "RETREAT_ZOMBIES";
				retreatDirection = rc.getLocation().directionTo(hostile.location).opposite();
			}
		}
		
		for(RobotInfo friendly : nearbyFriendlies) {
			if(friendly.type.equals(RobotType.TURRET)) {
				nearbyTurrets += 1;
			}
			else if(friendly.type.equals(RobotType.GUARD) || friendly.type.equals(RobotType.SOLDIER)) {
				nearbyPawns += 1;
			}
		}
		
		searchForParts();
		
	}
	
	
	private void handleMessages(Signal[] messages) {
		for(Signal message : messages) {
			
		}
	}
	
	/**
	 * Tries to build the specified RobotType in any direction.
	 * @param typeToBuild type of robot to build
	 * @throws GameActionException 
	 */
	private void tryToBuild(RobotType typeToBuild) throws GameActionException {
        // Choose a random direction to try to build in
        Direction dirToBuild = Utility.getRandomDirection(rand);
        for (int i = 0; i < 8; i++) {
            // If possible, build in this direction
            if (rc.canBuild(dirToBuild, typeToBuild)) {
                rc.build(dirToBuild, typeToBuild);
                return;
            } else {
                // Rotate the direction to try
                dirToBuild = dirToBuild.rotateLeft();
            }
        }
	}
	
	/**
	 * Detects the largest parts pile in sight.
	 * @return MapLocation of largest parts pile, or null if none found.
	 */
	private MapLocation searchForParts() {
		MapLocation[] nearbyLocations = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), RobotType.ARCHON.sensorRadiusSquared);
		MapLocation bestPartsPile = null;
		double maxParts = 0;
		for(MapLocation location : nearbyLocations) {
			double parts = rc.senseParts(location);
			if(parts > maxParts) {
				bestPartsPile = location;
				maxParts = parts;
			}
		}
		if(bestPartsPile != null) {
			target = bestPartsPile;
			state = "APPROACH_PARTS";
		}
		return bestPartsPile;
	}
	
}
