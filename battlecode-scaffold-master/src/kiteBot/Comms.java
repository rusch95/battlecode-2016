package kiteBot;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * Static class for handling commmunications.
 * Do not instantiate.
 * @author Ryan
 */
public class Comms {

	/**
	 * Messaging format is as follows:
	 * 
	 * Least-significant 2 digits of the first int are the Message Code. They indicate the purpose of the message.
	 * Digits XXXX00 signify a message-specific piece of auxiliary information (a robot ID, a threat score, etc).
	 * 
	 * Second int encodes a MapLocation, if necessary.
	 * 
	 */
	
	//~~~~~~~~~~~~~~~~MESSAGE CODES~~~~~~~~~~~~~~~~~~~~
	
	//Recon messages
	public static final int DEN_FOUND = 66; //If a den is scouted
	public static final int DEN_DESTROYED = 67; //If a den is destroyed
	public static final int ENEMY_ARCHON_SIGHTED = 70; //If an archon is scouted
	public static final int PANIC = 75; //If there's a serious horde coming and more defenses are needed probably
	public static final int SCOUT_DYING = 40; //A scout's dying hail mary
	public static final int PARTS_FOUND = 44; //Found a parts pile
	
	public static final int FOUND_MINX = 45; //Found map boundaries
	public static final int FOUND_MAXX = 46;
	public static final int FOUND_MINY = 47;
	public static final int FOUND_MAXY = 48;
	
	//Targeting messages
	public static final int TURRET_ATTACK_HERE = 55; //For targeting outside a turret's sight radius
	
	//Archon commands
	public static final int PLEASE_EXPLORE = 20; //Designating a scout to be an explorer
	public static final int PLEASE_TARGET = 21; //Designating a scout to target for a turret
	public static final int PLEASE_BAIT = 22; //Designating a scout to be zombie bait
	
	public static final int ATTACK_DEN = 30; //Designating all units to attack a den location.
	public static final int ATTACK_ENEMY = 31; //Designating all units to attack an enemy location
	public static final int DONT_ATTACK = 35; //Designating all units to ignore an attack message
	
	public static final int MIGRATE = 37; //Designating ALL troops to regroup near here

	public static final int TURRET_MOVE = 38;
	public static final int TURRET_STOP = 39;
	
	/**
	 * Creates the first int of a message from the code digits and the aux digits
	 * @param code 2-digit int that is a valid Message Code
	 * @param aux 4-digit auxiliary information
	 * @return 6-digit message header
	 */
	public static int createHeader(int code, int aux) {
		return Integer.parseInt(String.valueOf(aux) + String.valueOf(code));
	}
	
	/**
	 * Creates a message header with no auxiliary information
	 * @param code 2-digit Message Code
	 * @return 6-digit encoded message header
	 */
	public static int createHeader(int code) {
		return Integer.parseInt("5000" + String.valueOf(code));
	}
	
	/**
	 * Returns the Message Code from a message header.
	 * @param message the message header
	 * @return the 2-digit message code
	 */
	public static int getMessageCode(int message) {
		return message%100;
	}
	
	/**
	 * Returns the auxiliary 4-digit information from a message header.
	 * @param message 6-digit message header
	 * @return 4-digit auxiliary information
	 */
	public static int getAux(int message) {
		return (message - (message%100))/100;
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

	/* Only works with delays greater than 0.05
	 * 
	 */
	public static int delayToRange(double coreDelay, int sightRange) {
		double x = (coreDelay - 0.05) / 0.03;
		return (int) (sightRange * (2 + x));
	}
	
	public static double rangeToDelay(int range, int sightRange) {
		double x = range / sightRange - 2;
		if (x < 0) {
			x = 0;
		}
		return 0.05 + 0.03 * x;
	}
}
