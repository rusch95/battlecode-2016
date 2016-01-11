package FSMbot_1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Scout implements Role {
	private final RobotController rc;
	private final Random rand;
    private final Team myTeam;
    private final Team otherTeam;
    private final MapLocation birthplace;
    private final HashMap<MapLocation, Integer> theMap;
    private final ArrayList<MapLocation> dens;
    
	public Scout(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
		this.birthplace = rc.getLocation();
		this.theMap = new HashMap<MapLocation, Integer>();
		this.dens = new ArrayList<MapLocation>();
	}
	
	@Override
	public void run() {
		while(true){
			try {
				scan();
				if(rc.isCoreReady()) {
					Utility.tryToMove(rc, Utility.getRandomDirection(rand));
				}
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield();
		}
		
	}
	
	private void scan() throws GameActionException {
		MapLocation[] nearbyLocations = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
		
		RobotInfo[] hostilesNearby = rc.senseHostileRobots(rc.getLocation(), -1);
		
		MapLocation bestPartsPile = null;
		double maxParts = 0;
		double totalParts = 0;
		
		for(RobotInfo robot : hostilesNearby) {
			if(robot.type == RobotType.ZOMBIEDEN && !dens.contains(robot.location)) {
				rc.setIndicatorDot(robot.location, 250, 0, 0);
				rc.setIndicatorLine(rc.getLocation(), robot.location, 255, 0, 0);
				rc.broadcastMessageSignal(66, Utility.encodeLocation(robot.location), 500);
				dens.add(robot.location);
			}
		}
		
		for(MapLocation location : nearbyLocations) {
			double parts = rc.senseParts(location);
			totalParts += parts;
			if(parts > maxParts) {
				bestPartsPile = location;
				maxParts = parts;
			}
		}
		
		if(totalParts >= 100) { //Broadcast parts location
			rc.setIndicatorDot(bestPartsPile, 0, 100, 0);
			rc.broadcastMessageSignal(45, Utility.encodeLocation(bestPartsPile), 100);
		}
		
	}
}
