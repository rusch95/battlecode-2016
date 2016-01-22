package qualbot;

import java.util.Random;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

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
	protected int minX;
	protected int maxX;
	protected int minY;
	protected int maxY;
    
    protected boolean minXFound = false;
    protected boolean maxXFound = false;
    protected boolean minYFound = false;
    protected boolean maxYFound = false;
	
	//STATE CONSTANTS
	public static final int SEIGING_DEN = 150;
	public static final int SEIGING_ENEMY = 155;
	
	public static final int ATTACK_MOVING = 160;
	
	public static final int DEFENDING_ARCHON = 180;
	
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
	}
	
	/**
	 * Will be run exactly once. Do not let this escape or the main run() will also escape and the robot will explode.
	 */
	public abstract void run();
	
	/**
	 * Handles the contents of a robot's signal queue
	 */
	protected abstract void handleMessages();
	
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
	 * @return MapLocation location to base targeting off of
	 * TODO: All of it
	 */
	protected RobotInfo getAttackTarget(RobotInfo[] targetList, int minRange, MapLocation location) {
		throw new UnsupportedOperationException();
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
	
}
