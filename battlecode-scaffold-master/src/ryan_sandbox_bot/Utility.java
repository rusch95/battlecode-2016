package ryan_sandbox_bot;

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
	 * Encodes a Map coordinate (x,y) as a single number xxxyyy for messaging purposes.
	 * TODO make this more bytecode-efficient
	 * @param loc map coord
	 * @return int representing encoded location
	 */
	public static int encodeLocation(MapLocation loc) {
		int yCoordLength = String.valueOf(GameConstants.MAP_MAX_HEIGHT).length();
		int mesg = loc.x * ( (int) Math.pow(10, yCoordLength) ) + loc.y; //Encodes as XXXX_YYYY for 4digit max height
		return mesg;
	}
	
	/**
	 * Decodes an encoded location message.
	 * @param message encoded
	 * @return MapLocation corresponding to message
	 */
	public static MapLocation decodeLocation(int message) {
		int yCoordLength = String.valueOf(GameConstants.MAP_MAX_HEIGHT).length();
		int y = message % ( (int) Math.pow(10, yCoordLength) );
		int x = (message - y) / ( (int) Math.pow(10, yCoordLength) );
		return new MapLocation(x,y);
	}
	
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
			if (weakness > weakestWeakness && robot.location.distanceSquaredTo(location) > 5) { //TODO magic number
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
