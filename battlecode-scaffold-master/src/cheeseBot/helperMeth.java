package cheeseBot;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class helperMeth {
	
	/**
	 * 
	 * @param list of RobotInfo to search through
	 * @param bot type to count by
	 * @return int representing number of type of robots in nearbyRobots
	 */
	public static int getNumberOfBotOfType(RobotInfo[] nearbyRobots, RobotType botType) {
		int numberOf = 0;
		for (RobotInfo bot : nearbyRobots) {
			if (bot.type == botType) {
				numberOf += 1;
			}
		}
		return numberOf;
	}
	
	//TODO Change so that it doesn't go infinite loop go right left right left
	public static void tryToMove(Direction forward, RobotController rc) throws GameActionException{
		final int[] tryDirections = {0,-1,1,-2,2,-3,3};
		if(rc.isCoreReady()){
			for(int deltaD:tryDirections){
				Direction maybeForward = Direction.values()[(forward.ordinal()+deltaD+8)%8];
				if(rc.canMove(maybeForward)){
					rc.move(maybeForward);
					return;
				}
			}
			if(rc.getType().canClearRubble()){
				//failed to move, look to clear rubble
				MapLocation ahead = rc.getLocation().add(forward);
				if(rc.senseRubble(ahead)>=GameConstants.RUBBLE_OBSTRUCTION_THRESH){
					rc.clearRubble(forward);
				}
			}
		}
	}
	
	public static void tryToMoveOre(Direction forward, RobotController rc) throws GameActionException{
		final int[] tryDirectionsFirst = {0,-1,1,};
		final int[] tryDirectionsSecond = {-2,2,-3,3};
		if(rc.isCoreReady()){
			for(int deltaD:tryDirectionsFirst){
				Direction maybeForward = Direction.values()[(forward.ordinal()+deltaD+8)%8];
				if(rc.canMove(maybeForward)){
					rc.move(maybeForward);
					return;
				}
			}
			if(rc.getType().canClearRubble()){
				//failed to move, look to clear rubble
				MapLocation ahead = rc.getLocation().add(forward);
				if(rc.senseRubble(ahead)>=GameConstants.RUBBLE_OBSTRUCTION_THRESH){
					rc.clearRubble(forward);
					return;
				}
			}
			for(int deltaD:tryDirectionsSecond){
				Direction maybeForward = Direction.values()[(forward.ordinal()+deltaD+8)%8];
				if(rc.canMove(maybeForward)){
					rc.move(maybeForward);
					return;
				}
			}
		}
	}
	
	public static RobotInfo getWeakestRobot(RobotInfo[] nearbyRobots) {
		//Returns weakest unit from an array of sensed robots
		if (nearbyRobots.length > 0) {
        	double minHealth = Double.POSITIVE_INFINITY;
        	RobotInfo weakestBot = null;
        	for (RobotInfo curBot : nearbyRobots){
        		//Iterating through to find weakest robot
        		if (curBot.health < minHealth) {
        			minHealth = curBot.health;
        			weakestBot = curBot;
        		}
        	return weakestBot;
        	}
		}
		return null;
	}
	
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
