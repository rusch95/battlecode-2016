package sprintTourneyBot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

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
	 * @return RobotInfo of robot with highest dps per health
	 * TODO Add heuristic for targeting infected and discriminate more among weaponless targets
	 */
	public static RobotInfo getTarget(RobotInfo[] robotsToSearch, RobotController rc) {
		double maxDamagePerHealth = -1;
		RobotInfo targetRobot = null;
		for(RobotInfo robot : robotsToSearch) {
			//Should handle case if no attack power
			double attackPower = (robot.attackPower > 0) ? 0 : robot.attackPower; 
			rc.setIndicatorString(0, String.valueOf(attackPower));
			double damagePerHealth = attackPower / robot.health;
			if (damagePerHealth > maxDamagePerHealth) {
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
	
}
