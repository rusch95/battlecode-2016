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
import battlecode.common.ZombieCount;
import battlecode.common.ZombieSpawnSchedule;

public class Guard implements Role {
	private final RobotController rc;
	private final Random rand;
    private final Team myTeam;
    private final Team otherTeam;
    private final RobotType myType;
    private final int attackRadius;
    private final MapLocation[] myArchons;
    private final MapLocation[] enemyArchons;
    
    //Previou stat info
    private Direction prevDirection = Direction.NONE;
    private double prevHealth;
    
    //Magic Numbers
    private final int CLOSE_RANGE = 5;
    private final int MED_RANGE = 25;
    private final int FAR_RANGE = 30;
    private final int MAX_RANGE = -1;
	private final int CLOSE_TOO_MANY = 6;
	private final int CLOSE_TOO_FEW = 2;
	private final int MED_TOO_MANY = 20;
	private final int MED_TOO_FEW = 5;
	private final int FAR_TOO_MANY = 999;
	private final int FAR_TOO_FEW = 0;
	private final int MIN_SQUAD_NUM = 1;
	private final double RETREAT_HEALTH_PERCENT = 0.35;	
	private final int WITHIN_DEN_RANGE = 10;	
	private final int BASIC_GET_HELP_RANGE = 200;
	private final int DONT_FOLLOW_BASIC_IN_BASE_DISTANCE = 16;
	private final int REACHED_GOAL_DISTANCE = 16;
    private final int DONT_REBROADCAST_DISTANCE = 16;
	
    //Global Flags
    private boolean protectingBase = false;
    private boolean actingOnMessage = false;
    private boolean runningAway = false;
    private boolean beingAttacked = false;
    private boolean beingSniped = false;
    private boolean sentMessage = false;
    private boolean attackDen = false;
    private boolean zombieSpawnSoon = false;
    
    //Global Locations
    private MapLocation base;
    private MapLocation currentBasicGoal;
    private MapLocation currentOrderedGoal;
    private MapLocation curDenLocation;
    
    //Global Numbers
    private int basicGoalTimeout = 0;
    
	public Guard(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
		this.base = rc.getLocation();
		//TODO update base location if archon changes position
		this.prevHealth = rc.getHealth();
		this.myType = rc.getType();
		this.attackRadius = myType.attackRadiusSquared;
		this.myArchons = rc.getInitialArchonLocations(myTeam);
		this.enemyArchons = rc.getInitialArchonLocations(otherTeam);
	}
	
	@Override
	public void run() {
		while(true){
			try {
				RobotInfo[] enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), RobotType.GUARD.attackRadiusSquared);
				RobotInfo[] enemiesSeen = rc.senseHostileRobots(rc.getLocation(), MAX_RANGE);
				RobotInfo[] friendsSeen = rc.senseNearbyRobots(MAX_RANGE, myTeam);
				
				//Change some flags if necessary
				beingAttacked = (rc.getHealth() < prevHealth);
				beingSniped = (enemiesSeen.length == 0 && beingAttacked);
								
				Utility.cyanidePill(rc);
				
				RobotInfo targetEnemy = null;
				if(enemiesWithinRange.length > 0 && rc.isWeaponReady()) { //We're in combat
					targetEnemy = Utility.getTarget(enemiesWithinRange, 0, rc.getLocation());
					if(targetEnemy != null) {
						rc.attackLocation(targetEnemy.location);
						if (targetEnemy.type == RobotType.ZOMBIEDEN) {
							curDenLocation = targetEnemy.location;
						}
					} 
				}
				
				//Update whether zombies will spawn in next n rounds
				
				//Amortizes bytecode usage
				if (!rc.isCoreReady()) {
					handleMessages();
				} else {
					//Flee code
					Direction dirToGo = Direction.NONE;
					
					//Move back code
				    if (enemiesSeen.length > 0) {
							//Move towards enemy
							RobotInfo closeEnemy = Utility.getClosest(enemiesSeen, rc.getLocation());
							prevDirection = Utility.tryToMove(rc, rc.getLocation().directionTo(closeEnemy.location), prevDirection);
						
					//TODO Replace with comm and scout orders
				    } else if (currentOrderedGoal != null && rc.canSense(currentOrderedGoal)) {
				    	if (attackDen) {
				    		RobotInfo robotAtLocation = rc.senseRobotAtLocation(currentOrderedGoal);
				    		if (robotAtLocation == null || robotAtLocation.type != RobotType.ZOMBIEDEN) {
				    			currentOrderedGoal = base;
				    			attackDen = false;
				    		}
				    	}		
							
				    } else if (currentBasicGoal != null) {
						dirToGo = rc.getLocation().directionTo(currentBasicGoal);
						prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);
					
				    } else if (currentOrderedGoal != null) {
						dirToGo = rc.getLocation().directionTo(currentOrderedGoal);
						prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);	
					
				    } else {
						
						swarmMovement();
						
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
					MapLocation loc;
					switch (code){
						case Comms.ATTACK_DEN:
							loc = Comms.decodeLocation(contents[1]);
							currentOrderedGoal = loc;
							curDenLocation = loc;
							beingSniped = true;
							attackDen = true;
							break;
						case Comms.ATTACK_ENEMY:
							loc = Comms.decodeLocation(contents[1]);
							currentOrderedGoal = loc;
							break;
						case Comms.MIGRATE:
							loc = Comms.decodeLocation(contents[1]);
							currentOrderedGoal = loc;
							break;
					}
				}
				else { //Basic Message
					
				}
			}
		}
	}
	
	private Direction swarmMovement() throws GameActionException{
		
		RobotInfo[] closeFriends = rc.senseNearbyRobots(CLOSE_RANGE, myTeam); //Magic number
		RobotInfo[] medFriends = rc.senseNearbyRobots(MED_RANGE, myTeam); //More magic
		RobotInfo[] friendsSeen = rc.senseNearbyRobots(MAX_RANGE, myTeam);
		
		RobotInfo weakFriend = Utility.getWeakest(friendsSeen);
		RobotInfo closestArchon = Utility.getBotOfType(friendsSeen, RobotType.ARCHON, rand, rc);
		if (closestArchon != null) base = closestArchon.location;
		
		
		//TODO Change bug where this will also include friends who broadcasted leading to their high weapon delay
		Direction dirToGo = Direction.NONE;
		if (medFriends.length > MIN_SQUAD_NUM && weakFriend != null  && weakFriend.weaponDelay > 1 && (weakFriend.type != RobotType.ARCHON || Utility.chance(rand, .6))) {
			//Let's see if we have enough friends nearby
			//to assault enemies attacking team mates
			dirToGo = rc.getLocation().directionTo(weakFriend.location);
			prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);
			
	    } else if (closeFriends.length > CLOSE_TOO_MANY && Utility.chance(rand, .5)) {
			//Spread Apart if too many units adjacent
			dirToGo = Utility.getRandomDirection(rand);
			prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);
			
		} else if (closeFriends.length < CLOSE_TOO_FEW && Utility.chance(rand, .5)) {
			//Come together if med range is sparse
			RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc.getLocation());
			if (Utility.chance(rand, .8) && closestFriend != null) {
				//Whether to clump or go home
				dirToGo = rc.getLocation().directionTo(closestFriend.location);
			} else {
				dirToGo =  rc.getLocation().directionTo(base);
			}
			prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);	
			
		} else if (medFriends.length > MED_TOO_MANY && Utility.chance(rand, .5)) {
			//Come together if med range is sparse
			dirToGo = Utility.getRandomDirection(rand);
			prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);		
			
		} else if (medFriends.length < MED_TOO_FEW && Utility.chance(rand, .5)) {
			//Come together if med range is sparse
			RobotInfo closestFriend = Utility.getClosest(friendsSeen, rc.getLocation());
			if (Utility.chance(rand, .8) && closestFriend != null) {
				//Whether to clump or go home
				dirToGo = rc.getLocation().directionTo(closestFriend.location);
			} else {
				dirToGo =  rc.getLocation().directionTo(base);
			}
			prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);
		}
		return dirToGo;
	}
}
