package kiteBot;

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
import kiteBot.Utility.Tuple;

public class Archon implements Role {
	private RobotController rc;
	private Random rand;
	private final Team myTeam;
	private final Team otherTeam;
	private final MapLocation[] myArchons;
	private final MapLocation[] enemyArchons;
	private MapLocation target;
	
	
	private ArrayList<Integer> turrets = new ArrayList<>();
	
	//Map information
	private ArrayList<MapLocation> dens = new ArrayList<>();
	
    private int minX = 0;
    private int maxX = Integer.MAX_VALUE;
    private int minY = 0;
    private int maxY = Integer.MAX_VALUE;
    
    //Magic numbers
    private double DPS_RETREAT_THRESHOLD = 3;
    private double SHIT_BRICKS = 100;
    
    //Previous State Info
    private double prevHealth;
    private Direction prevDirection = Direction.NONE;
    private MapLocation lastSeenEnemyLoc = null;
    private MapLocation startingPos;

    
    //Global Flags
    private boolean minXFound = false;
    private boolean maxXFound = false;
    private boolean minYFound = false;
    private boolean maxYFound = false;
    private boolean denDestructionConfirmed = false;
    private boolean beingAttacked = false;
    private boolean beingSniped = false;
    private boolean fleeMode = false;
	
	private int scoutsKilled = 0;
	private ArrayList<Integer> deadScouts = new ArrayList<>();
    
	//Scan info
	private int nearbyBio = 0;
	private int nearbyTurrets = 0;
	
	//Recon Request stuff
	private boolean reconRequested;
	private int reconRequestTimeout = 0;
	private MapLocation reconLocation;
	
	//This is Sprint 1.0
	public Archon(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
		this.prevHealth = rc.getHealth();
		this.myArchons = rc.getInitialArchonLocations(myTeam);
		this.enemyArchons = rc.getInitialArchonLocations(otherTeam);
		this.startingPos = rc.getLocation();
		try {
			//Quick fix to make sure not in corner
			Direction dirToGo = rc.getLocation().directionTo(enemyArchons[0]);
			prevDirection = Utility.tryToMove(rc, dirToGo, prevDirection);
		} catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
    	}
	}
	
	@Override
	public void run() {
		while(true){
			try {				
				
				//Set some flags
				beingAttacked = (rc.getHealth() < prevHealth);
				prevHealth = rc.getHealth();
				
				handleMessages();
				scanArea();
				Tuple partsLoc = searchForParts();
				Tuple neutralBot = searchForNeutral();
				
				
				
				if(reconRequested) {
					if(tryToBuild(RobotType.SCOUT)) {
						rc.broadcastMessageSignal(Comms.createHeader(Comms.PLEASE_TARGET), Comms.encodeLocation(reconLocation), 2);
						reconRequestTimeout += 10;
						reconRequested = false;
					}
				}
				
				if (Utility.chance(rand, .25) && rc.getTeamParts() > RobotType.TURRET.partCost) {
					if(nearbyBio < 3) {
						tryToBuild(RobotType.GUARD);
						
					}
					else if(nearbyBio <= nearbyTurrets || Utility.chance(rand, .8)) {
						if (Utility.chance(rand, .9)) {
							tryToBuild(RobotType.SOLDIER);
						}
						else{
							tryToBuild(RobotType.GUARD);
						}
					}
					else if(Utility.chance(rand, 0.5)) {
						tryToBuild(RobotType.TURRET);						
					}
					
					if(scoutsKilled > 0 && weNeedExplorers() && Utility.chance(rand, 0.2)) {
						if(tryToBuild(RobotType.SCOUT)) scoutsKilled -= 1;
					}
				}
				
		        healAlly();
		        
				RobotInfo[] enemies = rc.senseHostileRobots(rc.getLocation(), -1);
				
				if (enemies.length == 0 && beingAttacked) {
					beingSniped = true;
				} else if (enemies.length > 0){
					beingSniped = false;
					lastSeenEnemyLoc = enemies[0].location;
				} else {
					beingSniped = false;
				}
				
				//TODO Optimize
				if (beingSniped) {
					if (lastSeenEnemyLoc != null && Utility.chance(rand, .5)) {
						Direction dirToGo = rc.getLocation().directionTo(lastSeenEnemyLoc).opposite();
						prevDirection = Utility.tryToMove(rc, dirToGo, prevDirection);
					} else {
						Direction dirToGo = Utility.getRandomDirection(rand);
						prevDirection = Utility.tryToMove(rc, dirToGo, prevDirection);
					}
				}
				
				int[] slice = {-1,0,1};
				Utility.Tuple<Direction, Double> dpsDirTuple = Utility.getDirectionOfMostDPS(enemies, rc, slice);
				if (dpsDirTuple != null) {
					Direction dirDps = dpsDirTuple.x;
					double dps = dpsDirTuple.y;
					if (dps > DPS_RETREAT_THRESHOLD) {
						fleeMode = (dps > SHIT_BRICKS);
						prevDirection = Utility.tryToMove(rc, dirDps.opposite(), prevDirection);
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
		nearbyTurrets = 0;
		nearbyBio = 0;
		RobotInfo[] friendlies = rc.senseNearbyRobots(-1, myTeam);
		for(RobotInfo friendly : friendlies) {
			if(friendly.type.equals(RobotType.TURRET) || friendly.type.equals(RobotType.TTM)) {
				if(!turrets.contains(friendly.ID)) {
					turrets.add(friendly.ID);
				}
				nearbyTurrets++;
			}
			else if(friendly.type.equals(RobotType.GUARD) || friendly.type.equals(RobotType.SOLDIER)) {
				nearbyBio++;		
			}
		//TODO Check for sniping	
			
		}
	}
	
	/**
	 * Handles the contents of the signal queue.
	 */
	private void handleMessages() {
		Signal[] messages = rc.emptySignalQueue();
		for(Signal message : messages) {
			if(message.getTeam().equals(myTeam)){ //Friendly message
				int[] contents = message.getMessage();
				int id = message.getID();
				if(contents != null) { //Not a basic signal
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
				else { //Basic Message
					if(turrets.contains(id) && reconRequestTimeout == 0 && Utility.chance(rand, 0.5)) { //Turret needs recon
						reconRequestTimeout = 20;
						reconRequested = true;
						reconLocation = message.getLocation();
					} else {
						
					}
				}
			}
		}
		if(reconRequestTimeout > 0) reconRequestTimeout -= 1;
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
	 * Calculates if we really need to make more scouts right now
	 * @return true if explorers would provide much benefit at all
	 */
	private boolean weNeedExplorers() {
		int sum = 0;
		if(minXFound) sum += 1;
		if(maxXFound) sum += 1;
		if(minYFound) sum += 1;
		if(maxYFound) sum += 1;
		if(dens.size() > 2) sum += 1;
		return (sum < 4);
	}
	
	/**
	 * Detects the largest/closest parts pile in sight.
	 * @return Tuple of form x = partsLocation, y = numberOfParts.
	 */
	private Tuple<MapLocation, Double>  searchForParts() {
		MapLocation[] partsLocations = rc.sensePartLocations(-1);
		MapLocation bestPartsPile = null;
		double maxParts = 0;
		for(MapLocation location : partsLocations) {
			//TODO Optimize best parts heuristic, such as adding in penalty for parts being in rubble
			int distance = rc.getLocation().distanceSquaredTo(location);
			double parts = rc.senseParts(location);
			if(parts / Math.pow(distance, .5) > maxParts) {
				bestPartsPile = location;
				maxParts = parts;
			}
		}
		Tuple<MapLocation, Double> locAndParts = new Tuple<>(bestPartsPile, maxParts);
		return locAndParts;
	}
	/**
	 * Returns the closests/best neutral bot in range.
	 * @return Tuple of the form Tuple.x = bestBot and Tuple.y = bestValue
	 */
	private Tuple<RobotInfo, Double> searchForNeutral() {
		RobotInfo[] neutralBots = rc.senseNearbyRobots(-1, Team.NEUTRAL);
		RobotInfo bestBot = null;
		double bestValue = 0;
		for(RobotInfo robot : neutralBots) {
			//TODO Create actual heuristic
			int distance = rc.getLocation().distanceSquaredTo(robot.location);
			double value = rc.getHealth() / distance; //Temp heuristic that should value archons
			if (value > bestValue) {
				bestValue = value;
				bestBot = robot;
			}
		}
		Tuple<RobotInfo, Double> robotAndValue = new Tuple<>(bestBot, bestValue);
		return robotAndValue;
	}
	
	/**
	 * Heals the weakest nearby friendly
	 * @throws GameActionException 
	 */
	public void healAlly() throws GameActionException {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(RobotType.ARCHON.attackRadiusSquared, rc.getTeam());
		RobotInfo weakestFriend = null;
		if (nearbyRobots.length > 0) {
        	double minHealth = Double.POSITIVE_INFINITY;
        	for (RobotInfo curBot : nearbyRobots){
        		if (curBot.health < minHealth && curBot.type != RobotType.ARCHON) {
        			minHealth = curBot.health;
        			weakestFriend = curBot;
        		}
        	}
		}
		if (weakestFriend != null) {
        	rc.repair(weakestFriend.location);
        }
	}
	
}