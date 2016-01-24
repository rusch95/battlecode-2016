package qualbot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;
import battlecode.common.ZombieSpawnSchedule;

public abstract class Role {
	
	//STATUS INFORMATION
	protected int state;
	protected MapLocation targetFlag;
	
	//ESSENTIAL FIELDS
	protected final RobotController rc;
	protected final Random rand;
	
	//ROBOT CHARACTERISTICS
	protected final RobotType type;
	protected final int minRange;
	protected final int attackRadiusSquared;
	protected final int sensorRadiusSquared;
	protected final Team myTeam;
	protected final Team otherTeam;
	protected final MapLocation birthplace;
	
	//MAP BOUNDS
	protected int minX = 0;
	protected int maxX = Integer.MAX_VALUE;
	protected int minY = 0;
	protected int maxY = Integer.MAX_VALUE;
    
    protected boolean minXFound = false;
    protected boolean maxXFound = false;
    protected boolean minYFound = false;
    protected boolean maxYFound = false;
	
    //MISC
    protected MapLocation myLocation;
    protected Signal[] messages;
    protected MapLocation[] friendlyArchonStartPositions;
    protected MapLocation[] enemyArchonStartPositions;
    protected ZombieSpawnSchedule spawnSchedule;
    protected int mapSymmetry; //0=XY,1=X, 2=Y, 3=XY
    protected MapLocation mapCenter;
    protected RobotInfo archonThatSpawnedMe;
    
    protected static final Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST,
    												Direction.EAST, Direction.SOUTH_EAST,
    												Direction.SOUTH, Direction.SOUTH_WEST,
    												Direction.WEST, Direction.NORTH_WEST};
    
	//STATE CONSTANTS
	public static final int SEIGING_DEN = 150;
	public static final int SEIGING_ENEMY = 155;
	public static final int IDLE = 100;
	
	/**
	 * Constructor for abstract class Role. Initializes final fields that all Role subclasses share.
	 * @param rc RobotController of the robot
	 */
	public Role(RobotController rc) {
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
		this.birthplace = rc.getLocation();
		this.type = rc.getType();
		if(type.equals(RobotType.TURRET) || type.equals(RobotType.TTM)) this.minRange = GameConstants.TURRET_MINIMUM_RANGE;
		else this.minRange = 0;
		this.attackRadiusSquared = type.attackRadiusSquared;
		this.sensorRadiusSquared = type.sensorRadiusSquared;
		this.friendlyArchonStartPositions = rc.getInitialArchonLocations(myTeam);
		this.enemyArchonStartPositions = rc.getInitialArchonLocations(otherTeam);
		this.spawnSchedule = rc.getZombieSpawnSchedule();
		Tuple<Integer, MapLocation> mapSymTup = symmetryAndCenter();
		this.mapSymmetry = mapSymTup.x; 
		this.mapCenter = mapSymTup.y;
		if (rc.getType() != RobotType.ARCHON) archonThatSpawnedMe = getBotOfType(rc.senseNearbyRobots(2, myTeam), RobotType.ARCHON);
	}
	
	/**
	 * Will be run exactly once. Do not let this escape or the main run() will also escape and the robot will explode.
	 */
	public abstract void run();
	
	/**
	 * Handles the contents of a robot's signal queue.
	 */
	protected void handleMessages(){
		messages = rc.emptySignalQueue();
		for(Signal message : messages) {
			handleMessage(message);
		}
	}
	
	/**
	 * Handles a single message.
	 */
	protected abstract void handleMessage(Signal message);
	
	/**
	 * Checks if it can avoid becomes being a zombie by killing itself, and does it if it can.
	 */
	protected void cyanidePill(){
		if(!rc.isInfected()) {
			double myHealth = rc.getHealth();
			if(myHealth <= 10) {
				MapLocation myLocation = rc.getLocation();
				RobotInfo[] zombiesNearby = rc.senseNearbyRobots(RobotType.RANGEDZOMBIE.attackRadiusSquared + 5, Team.ZOMBIE); //Senses for zombies within slightly more than a RangedZombie's attack range
				for(RobotInfo zombie : zombiesNearby) {
					if( myLocation.distanceSquaredTo(zombie.location) <= zombie.type.attackRadiusSquared + 5) {
						rc.disintegrate();
					}
				}
			}
		}
	}
	
	/**
	 * Returns the RobotInfo of the robot to be attacked out of the list provided.
	 * @param robotsToSearch array of RobotInfo to search through
	 * @param minRange min range to consider. 0 for most bots cept Turrets
	 * @param location location to search from
	 * @return RobotInfo to base targeting off of
	 */
	protected RobotInfo getAttackTarget(RobotInfo[] targetList, int minRange, MapLocation location) {
		RobotInfo bestTarget = null;
		double bestScore = -1;
		for(RobotInfo target : targetList) {
			if(target.location.distanceSquaredTo(location) >= minRange) { //We can hit them
				double score = target.attackPower/(Math.max(1,target.type.attackDelay) * target.health); //DPS per health
				if(score > bestScore) bestTarget = target;
			}
		}
		return bestTarget;
	}
	
	/**
	 * Returns the RobotInfo of the weakest robot in the given array.
	 * @param robotsToSearch array of RobotInfo to search through.
	 * @return robot with most missing health.
	 */
	protected static RobotInfo getWeakest(RobotInfo[] robotsToSearch) {
		double weakestWeakness = -1;
		RobotInfo weakestRobot = null;
		for(RobotInfo robot : robotsToSearch) {
			double weakness = robot.maxHealth - robot.health;
			if (weakness > weakestWeakness) {
				weakestWeakness = weakness;
				weakestRobot = robot;
			}
		}
		return weakestRobot;
	}
	
	/**
	 * Returns true or false with a given probability.
	 * @param prob probability of returning true
	 * @return result
	 */
	protected boolean chance(double prob) {
		return (rand.nextDouble() <= prob);
	}
	
	/**
	 * Moves in the optimal direction to avoid nearby enemies.
	 * myLocation must be up to date.
	 * @throws GameActionException 
	 */
	protected void dodgeEnemies() throws GameActionException{
		if(rc.isCoreReady()) {
			int lowestThreat = Integer.MAX_VALUE;
			int[] threats = {0,0,0,0,0,0,0,0};
			
			//Loops through all visible enemies at the moment, which might be a problem.
			for(RobotInfo enemy : rc.senseHostileRobots(myLocation, attackRadiusSquared)) {
				Direction danger = myLocation.directionTo(enemy.location);
				switch (danger) { //Change these values to scale with distance (and maybe dps)
					case NORTH:
						threats[0] += 1;
						break;
					case NORTH_EAST:
						threats[1] += 1;
						break;
					case EAST:
						threats[2] += 1;
						break;
					case SOUTH_EAST:
						threats[3] += 1;
						break;
					case SOUTH:
						threats[4] += 1;
						break;
					case SOUTH_WEST:
						threats[5] += 1;
						break;
					case WEST:
						threats[6] += 1;
						break;
					case NORTH_WEST:
						threats[7] += 1;
						break;
				}
			}
			
			//Account for obxtructions
			for(int i = 0; i < 8; i++) {
				double rubble = rc.senseRubble(myLocation.add(directions[i]));
				if(rubble > GameConstants.RUBBLE_OBSTRUCTION_THRESH) threats[i] += 5; //Change this to scale with rubble quantity
				else if( !rc.canMove(directions[i])) threats[i] = Integer.MAX_VALUE; //Some reason we can't move there other than rubble.
				else if(rubble > GameConstants.RUBBLE_SLOW_THRESH) threats[i] += 2;
			}
			
			//Find best option
			int directionIndex = 0;
			for(int i = 0; i < 8; i++) {
				if(threats[i] < lowestThreat) {
					lowestThreat = threats[i];
					directionIndex = i;
				}
			}
			Direction bestOption = directions[directionIndex];
	
			if(rc.canMove(bestOption)) {
				rc.move(bestOption);
				rc.setIndicatorString(2, "Moved: " + bestOption.toString());
			} else if(lowestThreat < Integer.MAX_VALUE) { //Must be rubbled
				rc.clearRubble(bestOption);
				rc.setIndicatorString(2, "Cleared Rubble: " + bestOption.toString());
			}
		}
	}
	
	/**
	 * 
	 * @param robotsToSearch
	 * @param type of robot to return
	 * @param rc
	 * @return robot that matches type criteria from list of robots
	 */
	protected static RobotInfo getBotOfType(RobotInfo[] robotsToSearch, RobotType type) {
		if (robotsToSearch.length == 0) {
			return null;
		}
		for (RobotInfo robot:robotsToSearch) {
			if (robot.type == type) {
				return robot;
			}
		}
		return null;
	}
	
	/**
	 * Calculates the map symmetry type and some of the center coordinates
	 * The center corresponds to the middle location between the groups of archons
	 * This only corresponds to the true center, when there is XY symmetry.
	 * For Y symmetry, the y coordinate is correct, and for X sym, the x coord is correct
	 * Finally, strong xy detects certain arrangements of the map, but is essentially the same as xy.
	 * @return Returns a Tuple of the form x = symmetry and y = map center constrained by symmetry. For symmetry, 0 = strong xy, 1 = x, 2 = y, and 3 = xy. 
	 */
	protected Tuple<Integer, MapLocation> symmetryAndCenter() {
		//Determine symmetry
		MapLocation archonOneLoc = friendlyArchonStartPositions[0];
		boolean symY = false;
		boolean symX = false;
		int totalX = 0; 
		int totalY = 0;
		for (MapLocation enemyArchonLoc : enemyArchonStartPositions) {
			if (archonOneLoc.x == enemyArchonLoc.x) {
				symX = true;
			}
			if (archonOneLoc.y == enemyArchonLoc.y) {
				symY = true;
			}
			totalX += enemyArchonLoc.x;
			totalY += enemyArchonLoc.y;	
		}
		for (MapLocation loc : friendlyArchonStartPositions) {
			totalX += loc.x;
			totalY += loc.y;
		}
		
		totalX /= friendlyArchonStartPositions.length * 2; //The average of all positions is the center
		totalY /= friendlyArchonStartPositions.length * 2;
		MapLocation center = new MapLocation(totalX, totalY);
		int sym;
		if (symY && symX) {
			sym = 0;
		} else if (symX) {
			sym = 1;
		} else if (symY) {
			sym = 2;
		} else {
			sym = 3;
		}
		Tuple<Integer, MapLocation> symAndCenter = new Tuple<>(sym, center);
		return symAndCenter;
	}
	
	public static class Tuple<X, Y> {
		public final X x;
		public final Y y;
		public Tuple(X x, Y y) {
			this.x = x;
			this.y = y;
		}
	}
}
