package sprintTourneyBot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
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
    private final ArrayList<MapLocation> enemyArchons;
    private final int state;
    private MapLocation target;
    
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
		this.enemyArchons = new ArrayList<MapLocation>();
	}
	
	@Override
	public void run() {
		while(true){
			try {
				handleMessages();
				if(state == EXPLORING) {
					rc.setIndicatorString(0, "I am EXPLORING");
					scan();
					if(target == null || rc.getLocation().distanceSquaredTo(target) < 4) { //Magic number
						reassignTarget();
					}
					moveTowardsTarget();
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
			if(hostile.type.equals(RobotType.ZOMBIEDEN) && !dens.contains(hostile.location)) { //New den sighted
				dens.add(hostile.location);
				rc.broadcastMessageSignal(Comms.createHeader(Comms.DEN_FOUND), Comms.encodeLocation(hostile.location), 100); //TODO change the range to something smarter
			}
			else if(hostile.type.equals(RobotType.ARCHON) && !enemyArchons.contains(hostile.location)) { //Enemy Archon sighted TODO: make the archon tracking smarter (by ids or something)
				enemyArchons.add(hostile.location);
				rc.broadcastMessageSignal(Comms.createHeader(Comms.ENEMY_ARCHON_SIGHTED), Comms.encodeLocation(hostile.location), 100); //TODO change the range to something smarter
			}
			
		}
		
	}
	
	/**
	 * Move towards the currently set target
	 * @throws GameActionException 
	 */
	private void moveTowardsTarget() throws GameActionException {
		Utility.tryToMove(rc, rc.getLocation().directionTo(target));
	}
	
	private void reassignTarget() {
		for(int i = 0; i < 100; i++) { //Make 100 target attempts
			MapLocation candidate = rc.getLocation().add(rand.nextInt(40)-20, rand.nextInt(40)-20);
			if(!theMap.containsKey(candidate)){
				target = candidate;
				return;
			}
		}
	}
	
}
