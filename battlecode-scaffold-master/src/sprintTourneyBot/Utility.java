package sprintTourneyBot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

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
	 * Returns the RobotInfo of the robot with highest dps per health
	 * @param robotsToSearch array of RobotInfo to search through
	 * @param minRange min range to consider
	 * @return RobotInfo of robot with highest dps per health
	 * TODO Add heuristic for targeting infected and discriminate more among weaponless targets
	 */
	public static RobotInfo getTarget(RobotInfo[] robotsToSearch, int minRange, RobotController rc) {
		double maxDamagePerHealth = -1;
		RobotInfo targetRobot = null;
		for(RobotInfo robot : robotsToSearch) {
			//Miscellaneous factors for increasing weighting
			double miscFactors = 1;
			if (robot.type.equals(RobotType.VIPER))
				miscFactors *= 5;
			
			double attackDelay = robot.type.attackDelay;
			if (attackDelay <= 0)
				attackDelay = 1;
			
			// TODO Change attack power to have small additions, so different small value added for 
			double damagePerHealth = robot.attackPower / attackDelay / robot.health * miscFactors;
			if (damagePerHealth > maxDamagePerHealth && rc.getLocation().distanceSquaredTo(robot.location) > minRange) {
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
		int closest = 999999;
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
	
	private static final int[] directionsToTry = {0, -1, 1, -2, 2};
	
	/**
	 * Try to move the given robot in the given direction.
	 * @param rc RobotController for robot.
	 * @param forward Direction to attempt movement.
	 * @throws GameActionException 
	 */
	public static void tryToMove(RobotController rc, Direction forward) throws GameActionException {
		if(rc.isCoreReady()){
			for(int deltaD:directionsToTry){
				Direction attemptDirection = Direction.values()[(forward.ordinal()+deltaD+8)%8];
				if(rc.canMove(attemptDirection)){
					rc.move(attemptDirection);
					return;
				}
			}
			//failed all attempts. Clear rubble ahead of us if we can.
			MapLocation ahead = rc.getLocation().add(forward);
			if(rc.senseRubble(ahead)>=GameConstants.RUBBLE_OBSTRUCTION_THRESH){
				rc.clearRubble(forward);
			}
		}
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
		MapLocation myLocation = rc.getLocation();
		int[] numOfTypeInDirection = new int[8];
		for (RobotInfo robot : robotsToSearch) {
			Direction dirToRobot = myLocation.directionTo(robot.location);
			switch (dirToRobot) {
				case NORTH:      numOfTypeInDirection[0] += 1;  break;
				case NORTH_EAST: numOfTypeInDirection[1] += 1;  break;
				case EAST:       numOfTypeInDirection[2] += 1;  break;
				case SOUTH_EAST: numOfTypeInDirection[3] += 1;  break;
				case SOUTH:      numOfTypeInDirection[4] += 1;  break;
				case SOUTH_WEST: numOfTypeInDirection[5] += 1;  break;
				case WEST:       numOfTypeInDirection[6] += 1;  break;
				case NORTH_WEST: numOfTypeInDirection[7] += 1;  break;
				default:
			}
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
	
	public class Tuple<X, Y> {
		public final X x;
		public final Y y;
		public Tuple(X x, Y y) {
			this.x = x;
			this.y = y;
		}
	}
	
	/**
	 * Iterates through RobotInfo[] to return direction of most DPS
	 * TODO figure out better way of returning dps and 
	 * @param robotsToSearch
	 * @param rc
	 * @return Tricky for now. Returns an array. The 
	 */
	public static Direction getDirectionOfMostDPS(RobotInfo[] robotsToSearch, RobotController rc) {
		MapLocation myLocation = rc.getLocation();
		double[] dpsInDirection = new double[8];
		for (RobotInfo robot : robotsToSearch) {
			Direction dirToRobot = myLocation.directionTo(robot.location);
			double attackDelay = robot.type.attackDelay;
			if (attackDelay <= 0)
				attackDelay = 1;
			double dps = robot.attackPower / attackDelay;
			switch (dirToRobot) {
				case NORTH:      dpsInDirection[0] += dps; break;
				case NORTH_EAST: dpsInDirection[1] += dps; break;
				case EAST:       dpsInDirection[2] += dps; break;
				case SOUTH_EAST: dpsInDirection[3] += dps; break;
				case SOUTH:      dpsInDirection[4] += dps; break;
				case SOUTH_WEST: dpsInDirection[5] += dps; break;
				case WEST:       dpsInDirection[6] += dps; break;
				case NORTH_WEST: dpsInDirection[7] += dps; break;
				default:
			}
		}
		int max_index = -1;
		double max_value = 0;
		for (int i = 0; i < 8; i++) {
			if (dpsInDirection[i] > max_value) {
				max_value = dpsInDirection[i];
				max_index = i;
			}
		}
		if (max_index == -1) {
			return null;
		} else {
			Direction dir = directions[max_index];
			return null;
		}
	}
	
	public static boolean chance(Random rand, double chance) {
		return (rand.nextDouble() < chance);
	}

}
