package sprintTourneyBot;

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
	private MapLocation target;
	private ArrayList<MapLocation> dens = new ArrayList<>();
	
	//Map information
	private ArrayList<MapLocation> knownDens = new ArrayList<MapLocation>();
	
    private int minX = 0;
    private int maxX = Integer.MAX_VALUE;
    private int minY = 0;
    private int maxY = Integer.MAX_VALUE;
    
    private boolean minXFound = false;
    private boolean maxXFound = false;
    private boolean minYFound = false;
    private boolean maxYFound = false;
	
	private int scoutsKilled = 0;
	private ArrayList<Integer> deadScouts = new ArrayList<>();
    
	//Constants
	private static final int MIGRATION_TARGET_THRESHOLD = 4;
	private static final int DEN_SEIGE_THRESHOLD = 8;
	private static final int DEN_TOO_CLOSE_THRESHOLD = 20;
	private static final int PARTS_TARGET_THRESHOLD = 0;
	private static final int PANIC_THRESHOLD = 8;
	private static final int SQUAD_RADIUS = 7;
	private static final int MIN_SURROUNDING_TURRETS = 1;
	
	
	public Archon(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		try {
			tryToBuild(RobotType.SCOUT);
		} catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
    	}
	}
	
	@Override
	public void run() {
		while(true){
			try {
				handleMessages();
				rc.setIndicatorString(0, "Scouts needed: " + String.valueOf(scoutsKilled));
				RobotInfo[] enemies = rc.senseHostileRobots(rc.getLocation(), -1);
				rc.setIndicatorString(1, "My X bounds are: " + String.valueOf(minX) + String.valueOf(minXFound) + " and " + String.valueOf(maxX) + String.valueOf(maxXFound));
				rc.setIndicatorString(2, "My Y bounds are: " + String.valueOf(minY) + String.valueOf(minYFound) + " and " + String.valueOf(maxY) + String.valueOf(maxYFound));
				if(scoutsKilled > 0) {
					if(tryToBuild(RobotType.SCOUT)) scoutsKilled -= 1;
				}
				if (Utility.chance(rand, .25) && rc.getTeamParts() > 50) {
					if (Utility.chance(rand, .5)) {
						tryToBuild(RobotType.SOLDIER);
					} else {
						tryToBuild(RobotType.GUARD);
					}
				}
				
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
		
	}
	
	/**
	 * Handles the contents of the signal queue.
	 */
	private void handleMessages() {
		Signal[] messages = rc.emptySignalQueue();
		for(Signal message : messages) {
			if(message.getTeam().equals(myTeam)){ //Friendly message
				int[] contents = message.getMessage();
				if(contents != null) { //Not a basic signal
					int id = message.getID();
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
						case Comms.SCOUT_DYING:
							if(!deadScouts.contains(id)){
								scoutsKilled += 1;
								deadScouts.add(id);
							}
							break;
						
						
						
					}
				}
			}
		}
	}
	
	/**
	 * Tries to build the specified RobotType in any direction.
	 * @param typeToBuild type of robot to build
	 * @throws GameActionException 
	 * @return boolean whether or not the build was successful
	 */
	private boolean tryToBuild(RobotType typeToBuild) throws GameActionException {
        // Choose a random direction to try to build in
		if(rc.isCoreReady()){
	        Direction dirToBuild = Utility.getRandomDirection(rand);
	        for (int i = 0; i < 8; i++) {
	            // If possible, build in this direction
	            if (rc.canBuild(dirToBuild, typeToBuild)) {
	                rc.build(dirToBuild, typeToBuild);
	                return true;
	            } else {
	                // Rotate the direction to try
	                dirToBuild = dirToBuild.rotateLeft();
	            }
	        }
		}
		return false;
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
		return bestPartsPile;
	}
	
}
