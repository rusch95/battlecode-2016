package abstractionexamplebot;

import battlecode.common.*;

public interface RobotHandler {

	/**
	 * Run a single round as this robot
	 */
	public void run() throws GameActionException;
	
}
