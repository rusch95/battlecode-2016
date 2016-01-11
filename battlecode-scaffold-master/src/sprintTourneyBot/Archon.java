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
	private String state;
	private String nextState;
	private MapLocation target;
	
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
	
	
	public Archon(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
	}
	
	@Override
	public void run() {
		while(true){
			try {
				
				
				
				
				
				
				
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
		return bestPartsPile;
	}
	
}
