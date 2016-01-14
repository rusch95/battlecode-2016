package kiteBot;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;

public class Soldier implements Role {
	private final RobotController rc;
	private final Random rand;
    private final Team myTeam;
    private final Team otherTeam;
    private final MapLocation[] myArchons;

    //Previous state info
    private Direction prevDirection = Direction.NONE;
    private double prevHealth;
    
    //Global Flags
    private boolean protectingBase = false;
    private boolean actingOnMessage = false;
    private boolean runningAway = false;
    private boolean beingAttacked = false;
    private boolean beingSniped = false;
    private boolean sentMessage = false;
    
    //Global Locations
    private MapLocation base;
    private MapLocation currentBasicGoal;
    private MapLocation currentOrderedGoal;
    
    //Global Integers
    private int basicGoalTimeout = 0;
    
    //Magic Numbers
    private final int CLOSE_RANGE = 2;
    private final int MED_RANGE = 17;
    private final int FAR_RANGE = 25;
    private final int MAX_RANGE = -1;
	private final int CLOSE_TOO_MANY = 2;
	private final int CLOSE_TOO_FEW = 1;
	private final int MED_TOO_MANY = 10;
	private final int MED_TOO_FEW = 5;
	private final int FAR_TOO_MANY = 999;
	private final int FAR_TOO_FEW = 0;
	private final int MIN_SQUAD_NUM = 1;
	private final double RETREAT_HEALTH_PERCENT = 0.35;
	
	private final int BASIC_GET_HELP_RANGE = 200;
	private final int DONT_FOLLOW_BASIC_IN_BASE_DISTANCE = 16;
	private final int REACHED_GOAL_DISTANCE = 16;
    private final int DONT_REBROADCAST_DISTANCE = 16;
	
	public Soldier(RobotController rc){
		
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
		this.base = rc.getLocation();
		this.prevHealth = rc.getHealth();
		this.myArchons = rc.getInitialArchonLocations(myTeam);
	}
	
	@Override
	public void run() {
		while(true){
			try {
				RobotInfo[] enemiesSeen = rc.senseHostileRobots(rc.getLocation(), MAX_RANGE);
				RobotInfo[] friendsSeen = rc.senseNearbyRobots(MAX_RANGE, myTeam);
				RobotInfo[] enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), RobotType.SOLDIER.attackRadiusSquared);
				
				rc.setIndicatorString(1, " "+rc.getCoreDelay());
				//TEST
				if (!sentMessage) {
					sentMessage = true;
					double coreDelayNeeded = 2.008 - rc.getCoreDelay();
					rc.broadcastSignal(Comms.delayToRange(coreDelayNeeded, RobotType.SOLDIER.sensorRadiusSquared));
					rc.setIndicatorString(2, " "+rc.getCoreDelay());
					//Get 
				}
				//Change some flags if necessary
				if (rc.getHealth() < prevHealth) {
					beingAttacked = true;
				} else {
					beingAttacked = false;
				} prevHealth = rc.getHealth();
				
				if (enemiesSeen.length == 0 && beingAttacked) {
					beingSniped = true;
				} else {
					beingSniped = false;
				}
				
				//Attack code
				RobotInfo targetEnemy = null;
				if(enemiesWithinRange.length > 0 && rc.isWeaponReady()) { //We're in combat
					targetEnemy = Utility.getTarget(enemiesWithinRange, 0, rc.getLocation());
					if( targetEnemy != null) {
						rc.attackLocation(targetEnemy.location);
					}
				}
				
				//Broadcast code
				if (targetEnemy != null && basicGoalTimeout == 0 && Utility.chance(rand, .5)) {
					if (targetEnemy.team == otherTeam || targetEnemy.type == RobotType.ZOMBIEDEN) {
						rc.broadcastSignal(BASIC_GET_HELP_RANGE);
						currentBasicGoal = rc.getLocation();
						basicGoalTimeout = 20;
					}
				}
				
				//Who needs goals
				if (currentBasicGoal != null) {
					if (rc.getLocation().distanceSquaredTo(currentBasicGoal) < REACHED_GOAL_DISTANCE && basicGoalTimeout < 10 || basicGoalTimeout == 0) {
						currentBasicGoal = null;
					} else if (basicGoalTimeout > 0) {
						basicGoalTimeout--;
					}
				}
				
				if  (currentBasicGoal != null) {
					rc.setIndicatorLine(rc.getLocation(), currentBasicGoal, 255, 255, 0);
					rc.setIndicatorString(0, currentBasicGoal.x + " " + currentBasicGoal.y);
				}
				
				handleMessages();
				
				 //Flee code
			    if (rc.getHealth() /rc.getType().maxHealth < RETREAT_HEALTH_PERCENT) {	    	
					Direction dirToGo = Direction.NONE;
						if (Utility.chance(rand, .7)) {
							dirToGo = rc.getLocation().directionTo(base);
						} else if (Utility.chance(rand, .7) && enemiesWithinRange.length > 0) {
							dirToGo = rc.getLocation().directionTo(enemiesWithinRange[0].location).opposite();
						}
						prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);
						currentBasicGoal = null;
				
						
						
			    } else if (enemiesSeen.length > 0) {
					//Move into firing range or kite them
			    	//TODO Optimize the fuck out this. Preventing dying to ranged units
					RobotInfo target = Utility.getTarget(enemiesSeen, 0, rc.getLocation());
					int distanceToTarget = rc.getLocation().distanceSquaredTo(target.location);
					if (target.type == RobotType.ZOMBIEDEN && distanceToTarget < 10) {
						//We're in range. Do nothing
					} else if (distanceToTarget < (rc.getType().attackRadiusSquared - 1.4) && !protectingBase) {
						//KIIITTTEEEE
						prevDirection=Utility.tryToMove(rc, target.location.directionTo(rc.getLocation()),prevDirection);
					} else {
						prevDirection=Utility.tryToMove(rc, rc.getLocation().directionTo(target.location),prevDirection);
					}
					currentBasicGoal = null;
					
			    } else if (currentBasicGoal != null) {
					Direction dirToGo = rc.getLocation().directionTo(currentBasicGoal);
					prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);
				
			    } else if (currentOrderedGoal != null) {
					Direction dirToGo = rc.getLocation().directionTo(currentOrderedGoal);
					prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);	
					
				} else if (friendsSeen.length > 0) {
					
					swarmMovement();
				}
						
				//TODO Could replace with an offset of the direction moved
				if (rc.isWeaponReady()) {
					enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), RobotType.SOLDIER.attackRadiusSquared);
					if(enemiesWithinRange.length > 0) { //We're in combat
						targetEnemy = Utility.getTarget(enemiesWithinRange, 0, rc.getLocation());
						if( targetEnemy != null) {
							rc.attackLocation(targetEnemy.location);
						}
					}
				}
				
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield();
		}
	}
	//~~~~~~~~~~~~~~~~~END MAIN LOOP~~~~~~~~~~~~~~~~~~~~
	
	private void handleMessages() {
		Signal[] messages = rc.emptySignalQueue();
		for(Signal message : messages) {
			if(message.getTeam().equals(myTeam)){ //Friendly message
				int[] contents = message.getMessage();
				int id = message.getID();
				//TODO Include ignore bit to lower melee overhead
				if(contents != null) { //Not a basic signal
					int code = Comms.getMessageCode(contents[0]);
					int aux = Comms.getAux(contents[0]);
					MapLocation loc = Comms.decodeLocation(contents[1]);
					switch (code){
					}
				}
				else { //Basic Message
					//Treat as a goto request
					if (rc.getLocation().distanceSquaredTo(base) < DONT_FOLLOW_BASIC_IN_BASE_DISTANCE) {
						currentBasicGoal = null;
					} else if (rc.getLocation().distanceSquaredTo(message.getLocation()) < DONT_REBROADCAST_DISTANCE) {
						basicGoalTimeout = 20;					
					} else if (basicGoalTimeout == 0){
						currentBasicGoal = message.getLocation();
						basicGoalTimeout = (int) (rc.getLocation().distanceSquaredTo(currentBasicGoal) * 1.5); //Magic number
					}
				}
			}
		}
	}
	
	private void swarmMovement() throws GameActionException{
		
		RobotInfo[] closeFriends = rc.senseNearbyRobots(CLOSE_RANGE, myTeam); //Magic number
		RobotInfo[] medFriends = rc.senseNearbyRobots(MED_RANGE, myTeam); //More magic
		RobotInfo[] friendsSeen = rc.senseNearbyRobots(MAX_RANGE, myTeam);
		
		RobotInfo weakFriend = Utility.getWeakest(friendsSeen);
		RobotInfo closestArchon = Utility.getBotOfType(friendsSeen, RobotType.ARCHON, rand, rc);
		if (closestArchon != null) base = closestArchon.location;
		
		
		//TODO Change bug where this will also include friends who broadcasted leading to their high weapon delay
		if (medFriends.length > MIN_SQUAD_NUM && weakFriend != null  && weakFriend.weaponDelay > 1 && (weakFriend.type != RobotType.ARCHON || Utility.chance(rand, .6))) {
			//Let's see if we have enough friends nearby
			//to assault enemies attacking team mates
			Direction dirToGo = rc.getLocation().directionTo(weakFriend.location);
			prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);
			
	    } else if (closeFriends.length > CLOSE_TOO_MANY && Utility.chance(rand, .5)) {
			//Spread Apart if too many units adjacent
			Direction dirToGo = Utility.getRandomDirection(rand);
			prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);
			
		} else if (closeFriends.length < CLOSE_TOO_FEW && Utility.chance(rand, .5)) {
			//Come together if med range is sparse
			RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc.getLocation());
			Direction dirToGo = null;
			if (Utility.chance(rand, .8)) {
				//Whether to clump or go home
				dirToGo = rc.getLocation().directionTo(closestFriend.location);
			} else {
				dirToGo =  rc.getLocation().directionTo(base);
			}
			prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);	
			
		} else if (medFriends.length > MED_TOO_MANY && Utility.chance(rand, .5)) {
			//Come together if med range is sparse
			Direction dirToGo = Utility.getRandomDirection(rand);
			prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);		
			
		} else if (medFriends.length < MED_TOO_FEW && Utility.chance(rand, .5)) {
			//Come together if med range is sparse
			RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc.getLocation());
			Direction dirToGo = null;
			if (Utility.chance(rand, .8)) {
				//Whether to clump or go home
				dirToGo = rc.getLocation().directionTo(closestFriend.location);
			} else {
				dirToGo =  rc.getLocation().directionTo(base);
			}
			prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);
		}
	}
	
	
}
