package kiteBot;

import java.util.ArrayList;
import java.util.Random;

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

public class Scout implements Role {
	private final RobotController rc;
	private final Random rand;
    private final Team myTeam;
    private final Team otherTeam;
    private final MapLocation birthplace;
    private final ArrayList<MapLocation> dens;
    private final ArrayList<MapLocation> enemyArchons;
    private int state;
    private MapLocation target;
    private Direction prevDirection = Direction.NONE;
    
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
				rc.setIndicatorDot(target, 0, 150, 0);
				handleMessages();
				if(state == EXPLORING) {
					rc.setIndicatorString(0, "I am EXPLORING");
					scan();
					if(rc.getLocation().distanceSquaredTo(target) < 2 || rc.getRoundNum()%20 == 1) { //Magic number
						reassignTarget();
					}
					moveTowardsTarget();
				}
				else if(state == BAITING) {
					
				}
				else if(state == TARGETING) {
					rc.setIndicatorString(0, "I am TARGETING");
					if(rc.getLocation().distanceSquaredTo(target) > 2) {
						moveTowardsTarget();
					}
					findTarget(); //Time that this is done doesn't matter
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
						case Comms.PLEASE_TARGET:
							state = TARGETING;
							target = loc;
							break;
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
					}
				}
			}
		}
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
			Direction away = hostilesVeryNearby[0].location.directionTo(rc.getLocation());
			if(Utility.chance(rand, 0.3)) away = away.rotateLeft();
			else if(Utility.chance(rand, 0.3)) away = away.rotateRight();
			target = rc.getLocation().add(away, 3);
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
	
	/**
	 * Finds a target suitable for the nearest turret, and signals the turret.
	 * @throws GameActionException
	 */
	private void findTarget() throws GameActionException {
		RobotInfo turret = findNearestTurret();
		if(turret == null) {
			state = EXPLORING;
		}
		else {
			RobotInfo targetEnemy = Utility.getTarget(rc.senseHostileRobots(turret.location, RobotType.TURRET.attackRadiusSquared), GameConstants.TURRET_MINIMUM_RANGE, turret.location);
			if(targetEnemy != null) {
				rc.setIndicatorDot(targetEnemy.location, 250, 0, 0);
				rc.broadcastMessageSignal(Comms.createHeader(Comms.TURRET_ATTACK_HERE), Comms.encodeLocation(targetEnemy.location), 25);
			}
		}
	}
	
	/**
	 * Finds the nearest "reasonable" turret for recon adjustment purposes.
	 * @return RobotInfo of nearest reasonable turret
	 */
	private RobotInfo findNearestTurret() {
		RobotInfo[] friendlies = rc.senseNearbyRobots(30, myTeam);
		RobotInfo best = null;
		int bestDistance = Integer.MAX_VALUE;
		MapLocation loc = rc.getLocation();
		for(RobotInfo friendly : friendlies) {
			if(friendly.type == RobotType.TURRET || friendly.type == RobotType.TTM){
				int distance = loc.distanceSquaredTo(friendly.location);
				if(distance < bestDistance) {
					best = friendly;
					bestDistance = distance;
				}
			}
		}
		return best;
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
		prevDirection=Utility.tryToMove(rc, rc.getLocation().directionTo(target),prevDirection);
	}
	
	private void reassignTarget() {
		MapLocation candidate = putInMap(rc.getLocation().add(rand.nextInt(40)-20, rand.nextInt(40)-20));
		target = candidate;
	}
	
}
