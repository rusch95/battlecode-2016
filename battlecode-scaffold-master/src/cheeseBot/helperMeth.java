package cheeseBot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class helperMeth {
	
	public static int getNumberOfBotOfType(RobotInfo[] nearbyRobots, RobotType botType) {
		int numberOf = 0;
		for (RobotInfo bot : nearbyRobots) {
			if (bot.type == botType) {
				numberOf += 1;
			}
		}
		return numberOf;
	}
	
	public static void tryToMove(Direction forward, RobotController rc) throws GameActionException{
		final int[] tryDirections = {0,-1,1,-2,2};
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
}
