package sprintTourneyBot;

import java.util.ArrayList;
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
    private final ArrayList<MapLocation> dens;
    private final ArrayList<MapLocation> enemyArchons;
    private final int state;
    private MapLocation target;
    
    private final static int sensorRadiusSquared = RobotType.SCOUT.sensorRadiusSquared;
    private final static int sensorRadius = (int)Math.pow(sensorRadiusSquared, 0.5); //Rounded down? What does sqrt(53) even signify for range?
    
    private int minX = 0;
    private int maxX = Integer.MAX_VALUE;
    private int minY = 0;
    private int maxY = Integer.MAX_VALUE;
    
    private boolean minXFound = false;
    private boolean maxXFound = false;
    private boolean minYFound = false;
    private boolean maxYFound = false;
    
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
		
		this.target = rc.getLocation();
		
		this.birthplace = rc.getLocation();
		this.dens = new ArrayList<MapLocation>();
		this.enemyArchons = new ArrayList<MapLocation>();
	}
	
	@Override
	public void run() {
		while(true){
			try {
				int fate = rand.nextInt(1000);
				handleMessages();
				if(state == EXPLORING) {
					scan();
					if(rc.getLocation().distanceSquaredTo(target) < 2 || rc.getRoundNum()%20 == 1) { //Magic number
						reassignTarget();
					}
					rc.setIndicatorString(1, "My X bounds are: " + String.valueOf(minX) + String.valueOf(minXFound) + " and " + String.valueOf(maxX) + String.valueOf(maxXFound));
					rc.setIndicatorString(2, "My Y bounds are: " + String.valueOf(minY) + String.valueOf(minYFound) + " and " + String.valueOf(maxY) + String.valueOf(maxYFound));
					rc.setIndicatorDot(target, 250, 0, 0);
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
		RobotInfo[] hostilesNearby = rc.senseHostileRobots(rc.getLocation(), -1);
		RobotInfo[] hostilesVeryNearby = rc.senseHostileRobots(rc.getLocation(), 15);
		RobotInfo[] friendliesNearby = rc.senseNearbyRobots(20, myTeam);
		
		MapLocation[] partsNearby = rc.sensePartLocations(-1);
		MapLocation[] tilesNearby = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), sensorRadiusSquared);
		
		if(hostilesVeryNearby.length > 0) {
			target = rc.getLocation().add(hostilesVeryNearby[0].location.directionTo(rc.getLocation()), 3);
		}
		
		if(hostilesVeryNearby.length > 0 && rc.getHealth() < 15) { //HAIL MARY I'M DYING
			rc.setIndicatorString(0, "I'M DYING. HAIL MARY!!");
			rc.broadcastMessageSignal(Comms.createHeader(Comms.SCOUT_DYING), 0, 100000000);
		}
		
		scanForBounds();
		
		for(RobotInfo hostile : hostilesNearby) {
			if(hostile.type.equals(RobotType.ZOMBIEDEN) && !dens.contains(hostile.location)) { //New den sighted
				dens.add(hostile.location);
				rc.broadcastMessageSignal(Comms.createHeader(Comms.DEN_FOUND), Comms.encodeLocation(hostile.location), broadcastDistance());
			}
			else if(hostile.type.equals(RobotType.ARCHON) && !enemyArchons.contains(hostile.location)) { //Enemy Archon sighted TODO: make the archon tracking smarter (by ids or something)
				enemyArchons.add(hostile.location);
				rc.broadcastMessageSignal(Comms.createHeader(Comms.ENEMY_ARCHON_SIGHTED), Comms.encodeLocation(hostile.location), broadcastDistance());
			}
			
		}
		
	}
	
	/**
	 * Scans for Map Edges
	 * @throws GameActionException 
	 */
	private void scanForBounds() throws GameActionException {
		MapLocation cur = rc.getLocation();
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
				rc.broadcastMessageSignal(Comms.createHeader(Comms.FOUND_MINX, minX), 0, broadcastDistance());
				target = putInMap(target);
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
				rc.broadcastMessageSignal(Comms.createHeader(Comms.FOUND_MAXX, maxX), 0, broadcastDistance());
				target = putInMap(target);
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
				rc.broadcastMessageSignal(Comms.createHeader(Comms.FOUND_MINY, minY), 0, broadcastDistance());
				target = putInMap(target);
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
				rc.broadcastMessageSignal(Comms.createHeader(Comms.FOUND_MAXY, maxY), 0, broadcastDistance());
				target = putInMap(target);
			}
		}
	}
	
	private MapLocation putInMap(MapLocation location){
		int x, y;
		if(location.x < minX) x = minX;
		else if(location.x > maxX) x = maxX;
		else x = location.x;
		if(location.y < minY) y = minY;
		else if(location.y > maxY) y = maxY;
		else y = location.y;
		return new MapLocation(x,y);
	}
	
	
	/**
	 * Calculates a good distance to broadcast
	 * @return distance
	 */
	private int broadcastDistance() {
		return rc.getLocation().distanceSquaredTo(birthplace)+10;
	}
	
	/**
	 * Move towards the currently set target
	 * @throws GameActionException 
	 */
	private void moveTowardsTarget() throws GameActionException {
		Utility.tryToMove(rc, rc.getLocation().directionTo(target));
	}
	
	private void reassignTarget() {
		MapLocation candidate = putInMap(rc.getLocation().add(rand.nextInt(40)-20, rand.nextInt(40)-20));
		target = candidate;
	}
	
}
