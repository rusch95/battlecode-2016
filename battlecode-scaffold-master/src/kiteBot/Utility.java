package kiteBot;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

/**
 * Class full of static methods that are useful.
 * @author Ryan
 *
 */
public class Utility {
	
	/**
	 * Returns the RobotInfo of the weakest robot in the given array.
	 * @param robotsToSearch array of RobotInfo to search through.
	 * @return robot with most missing health.
	 */
	public static RobotInfo getWeakest(RobotInfo[] robotsToSearch) {
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
	 * This function calculates certain map info such as symmetry and the map center via initial archon lists
	 * Center is only partially true. The X coordinate is true with Y-Sym and Y is true with X-Sym.
	 * @return Returns a Tuple of the form x = symmetry and y = map center constrained by symmetry. For symmetry, 0 = strong xy, 1 = x, 2 = y, and 3 = xy. 
	 */
	public static Tuple<Integer, MapLocation> startingMapInfo(MapLocation[] myArchons, MapLocation[] otherArchons) {
		//Determine symmetry
		MapLocation myLoc = myArchons[0];
		boolean symY = false;
		boolean symX = false;
		int totalX = 0;
		int totalY = 0;
		for (MapLocation notMyLoc : otherArchons) {
			if (myLoc.x == notMyLoc.x) {
				symX = true;
			}
			if (myLoc.y == notMyLoc.y) {
				symY = true;
			}
			totalX += notMyLoc.x;
			totalY += notMyLoc.y;	
		}
		for (MapLocation loc : myArchons) {
			totalX += loc.x;
			totalY += loc.y;
		}
		
		totalX /= myArchons.length * 2;
		totalY /= myArchons.length * 2;
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
	
	/**
	 * Returns the RobotInfo of the robot with highest dps per health
	 * @param robotsToSearch array of RobotInfo to search through
	 * @param minRange min range to consider. 0 for most bots cept Turrets
	 * @param location location to search from
	 * @return MapLocation location to base targeting off of
	 * TODO Add heuristic for targeting infected and discriminate more among weaponless targets
	 * TODO Target enemy archons when it looks like zombie time
	 * TODO Implement distance heuristics
	 * TODO Add heuristic for targeting enemies hitting high values targets such as turrets
	 */
	public static RobotInfo getTarget(RobotInfo[] robotsToSearch, int minRange, MapLocation location) {
		double maxDamagePerHealth = -1;
		RobotInfo targetRobot = null;
		for(RobotInfo robot : robotsToSearch) {
			//Miscellaneous factors for increasing weighting
			double miscFactors = 1;
			double fudgeFactor = 0;
			//TODO Micromanage this to optimize, since it's inside an expensive, often called loop
			if (robot.type.equals(RobotType.VIPER)) {
				miscFactors *= 5;
			} else if (robot.type.equals(RobotType.BIGZOMBIE)) { //Heavy hitter should be taken down far away
				miscFactors *= 3;
			} else if (robot.type == RobotType.ARCHON) {
				fudgeFactor = .5;
			} else if (robot.type == RobotType.SCOUT) {
				//Scouts next to turrets should be super squashed to criple the turrets
				fudgeFactor = .3;
				miscFactors *= 5; 
			}
			int distance = location.distanceSquaredTo(robot.location);
			if (robot.team == Team.ZOMBIE) {
				miscFactors *= 10 / Math.pow(distance, .3333); //Magic Number
			} else {
				if (robot.viperInfectedTurns > 3 || robot.zombieInfectedTurns > 3) {
					miscFactors *= Math.pow(distance,.7); //Magic
				}
			}
			
			double attackDelay = robot.type.attackDelay;
			if (attackDelay <= 0)
				attackDelay = 1;
			
			// TODO Change attack power to have small additions, so different small value added for 
			double damagePerHealth = (robot.attackPower + fudgeFactor) / attackDelay / robot.health * miscFactors;
			if (damagePerHealth > maxDamagePerHealth && distance > minRange) {
				maxDamagePerHealth = damagePerHealth;
				targetRobot = robot;
			}
		}
		return targetRobot;
	}
	
	/**
	 * Returns the RobotInfo of the robot with highest dps per health that isn't already infected
	 * @param robotsToSearch array of RobotInfo to search through
	 * @param minRange min range to consider
	 * @return MapLocation location to base targeting off of
	 * TODO Add heuristic for targeting infected and discriminate more among weaponless targets
	 */
	public static RobotInfo getViperTarget(RobotInfo[] robotsToSearch, int minRange, MapLocation location) {
		double maxDamagePerHealth = -1;
		RobotInfo targetRobot = null;
		for(RobotInfo robot : robotsToSearch) {
			//Miscellaneous factors for increasing weighting
			int uninfectedFactor = 1;
			if (robot.viperInfectedTurns < 3) {
				uninfectedFactor = 5;
			}
			double fudgeFactor = 0;
			if (robot.type == RobotType.ARCHON) {
				fudgeFactor = .5;
			} else if (robot.type == RobotType.SCOUT) {
				//TODO Modify so that enemy scouts that are helping turtle are destroyed
				fudgeFactor = .3;
			}
			
			double miscFactors = 1;
			if (robot.type.equals(RobotType.VIPER))
				miscFactors *= 5;
			if (robot.team == Team.ZOMBIE)
				miscFactors *= .2;
			
			double attackDelay = robot.type.attackDelay;
			if (attackDelay <= 0)
				attackDelay = 1;
			
			// TODO Change attack power to have small additions, so different small value added for 
			double damagePerHealth = (robot.attackPower + fudgeFactor) / attackDelay / robot.health * miscFactors * uninfectedFactor;
			if (damagePerHealth > maxDamagePerHealth && location.distanceSquaredTo(robot.location) >= minRange) {
				maxDamagePerHealth = damagePerHealth;
				targetRobot = robot;
			}
		}
		return targetRobot;
	}
	
	/**
	 * Returns the RobotInfo of the weakest robot in the given array OUTSIDE OF A TURRET'S MIN RANGE.
	 * @param robotsToSearch array of RobotInfo to search through.
	 * @param location of the Turret
	 * @return robot with most missing health.
	 */
	public static RobotInfo getWeakestForTurret(RobotInfo[] robotsToSearch, MapLocation location) {
		
		double weakestWeakness = -1;
		RobotInfo weakestRobot = null;
		for(RobotInfo robot : robotsToSearch) {
			double weakness = robot.maxHealth - robot.health;
			if (weakness > weakestWeakness && robot.location.distanceSquaredTo(location) > GameConstants.TURRET_MINIMUM_RANGE) { 
				weakestWeakness = weakness;
				weakestRobot = robot;
			}
		}
		return weakestRobot;
	}
	
	/**
	 * Returns the closest robot in proximity to the given location.
	 * @param robotsToSearch array of robots to search through
	 * @param location epicenter of proximity
	 * @return RobotInfo of closest robot
	 */
	public static RobotInfo getClosest(RobotInfo[] robotsToSearch, MapLocation location) {
		if (robotsToSearch.length == 0) {
			return null;
		}
		int closest = Integer.MAX_VALUE;
		RobotInfo closeRobot = null;
		for (RobotInfo robot : robotsToSearch) {
			int distance = location.distanceSquaredTo(robot.location);
			if (distance < closest) {
				closest = distance;
				closeRobot = robot;
			}
		}
		return closeRobot;
	}
	
	public static RobotInfo getWeaponDelayed(RobotInfo[] robotsToSearch) {
		for (RobotInfo robot : robotsToSearch) {
			if (robot.weaponDelay > 1 && robot.type != RobotType.ARCHON){
				return robot;
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param robotsToSearch
	 * @param type of robot to return
	 * @param rand
	 * @param rc
	 * @return robot that matches type criteria from list of robots
	 */
	public static RobotInfo getBotOfType(RobotInfo[] robotsToSearch, RobotType type, Random rand, RobotController rc) {
		if (robotsToSearch.length == 0) {
			return null;
		}
		int index = rand.nextInt(robotsToSearch.length);
		for (int i = 0; i < robotsToSearch.length; i++) {
			if (robotsToSearch[(index + i) % robotsToSearch.length].type == type) {
				return robotsToSearch[(index + i) % robotsToSearch.length];
			}
		}
		return null;
	}
	
	/**
	 * Gives number of robots within a specified range of the robot
	 * @param robotsToSearch
	 * @param rc
	 * @param min range to consider
	 * @param max range to consider
	 * @return number of robots within range
	 */
	public static int getNumOfFriendsWithinRange(RobotInfo[] robotsToSearch, RobotController rc, int min, int max) {
		
		MapLocation myLocation = rc.getLocation();
		int numberOf = 0;
		for (RobotInfo robot : robotsToSearch) {
			int distance = myLocation.distanceSquaredTo(robot.location);
			if (min < distance && distance < max) {
				numberOf += 1;
			}
		}
		return numberOf;
	}
	
	public static int getNumberOfBotOfType(RobotInfo[] nearbyRobots, RobotType botType) {
		int numberOf = 0;
		for (RobotInfo bot : nearbyRobots) {
			if (bot.type == botType) {
				numberOf += 1;
			}
		}
		return numberOf;
	}
	
	private static final int[] directionsToTryFirst = {0, -1, 1};
	private static final int[] directionsToTrySecond = {-2, 2};
	
	/**
	 * Try to move the given robot in the given direction. Good now at plowing through rubble
	 * @param rc RobotController for robot.
	 * @param forward Direction to attempt movement.
	 * @throws GameActionException 
	 */
	public static Direction tryToMove(RobotController rc, Direction forward, Direction prevDirection) throws GameActionException {
		if(rc.isCoreReady()){
			for(int deltaD:directionsToTryFirst){
				Direction attemptDirection = Direction.values()[(forward.ordinal()+deltaD+8)%8];
				if(rc.canMove(attemptDirection)){
					rc.move(attemptDirection);
					return Direction.NONE;
				}
			}
			//failed all attempts. Clear rubble ahead of us if we can.
			double minRubble = 1000000;
			Direction dirToClear = Direction.NONE;
			for(int deltaD:directionsToTryFirst){
				Direction attemptDirection = Direction.values()[(forward.ordinal()+deltaD+8)%8];
				MapLocation ahead = rc.getLocation().add(attemptDirection);
				double rubbleAmount = rc.senseRubble(ahead);
				if(rc.onTheMap(ahead) && rubbleAmount >= GameConstants.RUBBLE_OBSTRUCTION_THRESH && rubbleAmount <= minRubble) {
					minRubble = rubbleAmount;
					dirToClear = attemptDirection;
				}
			}
			if (dirToClear != Direction.NONE) {
				rc.clearRubble(dirToClear);
				return Direction.NONE;
			}
			for(int deltaD:directionsToTrySecond){
				Direction attemptDirection = Direction.values()[(forward.ordinal()+deltaD+8)%8];
				if(rc.canMove(attemptDirection)){
					rc.move(attemptDirection);
					return attemptDirection.opposite();
				}
			}
		}
		return Direction.NONE;
	}
	
	private static final Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
												   Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

	/**
	 * Return a random direction, using the caller's Random object.
	 * @param rand Random
	 * @return a random Direction.
	 */
	public static Direction getRandomDirection(Random rand) {
		return directions[rand.nextInt(8)];
	}
	
	/**
	 * Returns the direction with the most of a certain type of robot
	 * @param robotsToSearch
	 * @param type
	 * @param rc
	 * @return
	 */
	public static Direction getDirectionOfType(RobotInfo[] robotsToSearch, RobotType type, RobotController rc) {
		//TODO refactor to use ordinal method instead of switch case. And do this for dps as well
		MapLocation myLocation = rc.getLocation();
		int[] numOfTypeInDirection = new int[8];
		for (RobotInfo robot : robotsToSearch) {
			Direction dirToRobot = myLocation.directionTo(robot.location);
			numOfTypeInDirection[dirToRobot.ordinal()]++;
		}
		int maxIndex = -1;
		int maxValue = 0;
		int widerView[] = new int[8];
		int viewToConsider[] = {-2,-1,0,1,2};
		//Directional Offsets
		for (int i = 0; i < 8; i++) {
			for (int offset : viewToConsider) {
				widerView[i] += numOfTypeInDirection[(i+offset+8)%8];
			}
		}
		for (int i = 0; i < 8; i++) {
			if (widerView[i] > maxValue) {
				maxValue = numOfTypeInDirection[i];
				maxIndex = i;
			}
		}
		if (maxIndex == -1) {
			return null;
		} else {
			return directions[maxIndex];
		}
	}
	/**
	 * Returns the delay to core and weapon delay from using too much bytecode
	 * @param bytecode The amount of bytecode used
	 * @param maxBytecode The max amount of bytecode this robot can use
	 * @return a double corresponding to the extra delay penalty.
	 */
	public static double bytecodeDelayPenalty(int bytecode, int maxBytecode) {
		double cost = Math.pow((bytecode + 8000 - maxBytecode)/8000,1.5);
		if (cost < 0) {
			return 0;
		} else {
			return cost;
		}
	}
	
	/**
	 * Filters an array for only robots of a certain type
	 * @param robotsToSearch array to filter
	 * @param filterType to filter for
	 * @return filtered array
	 */
	public static RobotInfo[] filterByType(RobotInfo[] robotsToSearch, RobotType filterType) {
		ArrayList<RobotInfo> robots = new ArrayList<RobotInfo>();
		for(RobotInfo robot : robotsToSearch){
			if(robot.type.equals(filterType)) robots.add(robot);
		}
		RobotInfo[] array = new RobotInfo[robots.size()];
		return robots.toArray(array);
	}
	
	/**
	 * Determines in which direction, given a group of robots, has the most dps
	 * TODO Make changes to the slice of area considered for dps and have it poss as a variable
	 * @param robotsToSearch
	 * @param rc
	 * @param viewToConsider integer list such as [-1, 0, 1] that includes the offsets in the direction 
	 * aka [0] only looks in that direction and [-4,-3,..,3,4] looks everywhere
	 * @return Returns a tuple such that Tuple.x = direction and Tuple.y = dps of that direction 
	 */
	public static Tuple<Direction, Double> getDirectionOfMostDPS(RobotInfo[] robotsToSearch, RobotController rc, int[] viewToConsider) {
		MapLocation myLocation = rc.getLocation();
		double[] dpsInDirection = new double[8];
		for (RobotInfo robot : robotsToSearch) {
			Direction dirToRobot = myLocation.directionTo(robot.location);
			double attackDelay = robot.type.attackDelay;
			if (attackDelay <= 0)
				attackDelay = 1;
			double dps = robot.attackPower / attackDelay;
			dpsInDirection[dirToRobot.ordinal()] += dps;
		}
		int maxIndex = -1;
		double maxValue = 0;
		int widerView[] = new int[8];
		//Directional Offsets
		for (int i = 0; i < 8; i++) {
			for (int offset : viewToConsider) {
				widerView[i] = (int) dpsInDirection[(i+offset+8)%8];
			}
		}
		for (int i = 0; i < 8; i++) {
			if (widerView[i] > maxValue) {
				maxValue = dpsInDirection[i];
				maxIndex = i;
			}
		}
		if (maxIndex == -1) {
			return null;
			
		} else {
			Direction dir = directions[maxIndex];
			double maxDps = dpsInDirection[maxIndex];
			Tuple<Direction, Double> dirAndDps = new Tuple<>(dir, maxDps);
			return dirAndDps;
		}
	}
	
	public static class Tuple<X, Y> {
		public final X x;
		public final Y y;
		public Tuple(X x, Y y) {
			this.x = x;
			this.y = y;
		}
	}
	
	/**
	 * Returns true with probability chance, and false with probability (1-chance)
	 * @param rand Random generator
	 * @param chance probability
	 * @return probabilistic boolean
	 */
	public static boolean chance(Random rand, double chance) {
		return (rand.nextDouble() <= chance);
	}

}


