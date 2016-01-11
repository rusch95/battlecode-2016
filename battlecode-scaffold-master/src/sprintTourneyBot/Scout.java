package sprintTourneyBot;

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
    private final int state;
    
    //Possible states
    private final static int EXPLORING = 1;
    private final static int BAITING = 2;
    private final static int TARGETING = 3;
    
	public Scout(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();

		this.state = EXPLORING;
		
		this.birthplace = rc.getLocation();
		this.theMap = new HashMap<MapLocation, Integer>();
		this.dens = new ArrayList<MapLocation>();
	}
	
	@Override
	public void run() {
		while(true){
			try {
				handleMessages();
				if(state == EXPLORING) {
					rc.setIndicatorString(0, "I am EXPLORING");
					scan();
					Utility.tryToMove(rc, Utility.getRandomDirection(rand)); //TODO Make this real
				}
				else if(state == BAITING) {
					
				}
				else if(state == TARGETING) {
					
				}
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield();
		}
		
	}
	//~~~~~~~~~~~~~~~~~~~~END MAIN LOOP~~~~~~~~~~~~~~~~~
	
	/**
	 * Handles the message queue for this bot
	 */
	private void handleMessages() {
		;;
	}
	
	/**
	 * Scans the sight range of the Scout
	 * @throws GameActionException
	 */
	private void scan() throws GameActionException {
		RobotInfo[] hostilesNearby = rc.senseHostileRobots(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
		RobotInfo[] friendliesNearby = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, myTeam);
		
		MapLocation[] tilesNearby = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
		
		for(RobotInfo hostile : hostilesNearby) {
			if(hostile.type.equals(RobotType.ZOMBIEDEN) && !dens.contains(hostile.location)) {
				dens.add(hostile.location);
				rc.broadcastMessageSignal(Comms.createHeader(Comms.DEN_FOUND), Comms.encodeLocation(hostile.location), 100); //TODO change the range to something smarter
			}
			
			
		}
		
	}
}
