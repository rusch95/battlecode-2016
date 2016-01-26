package seedingBot;

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
    private int squadArchon;
    private final MapLocation[] myArchons;
    private final MapLocation[] enemyArchons;

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
    private boolean attackDen = false;
    private boolean zombieSpawnSoon = false;
    
    //Global Locations
    private MapLocation base;
    private MapLocation currentBasicGoal;
    private MapLocation currentOrderedGoal;
    
    //Global Integers
    private int basicGoalTimeout = 0;
    
    //Magic Numbers
    private final int CLOSE_RANGE = 2;
    private final int MED_RANGE = 9;
    private final int FAR_RANGE = 25;
    private final int MAX_RANGE = -1;
	private final int CLOSE_TOO_MANY = 4;
	private final int CLOSE_TOO_FEW = 2;
	private final int MED_TOO_MANY = 8;
	private final int MED_TOO_FEW = 3;
	private final int FAR_TOO_MANY = 999;
	private final int FAR_TOO_FEW = 0;
	private final int MIN_SQUAD_NUM = 1;
	private final double RETREAT_HEALTH_PERCENT = 0.35;	
	private final int WITHIN_DEN_RANGE = 10;	
	private final int BASIC_GET_HELP_RANGE = 200;
	private final int DONT_FOLLOW_BASIC_IN_BASE_DISTANCE = 16;
	private final int REACHED_GOAL_DISTANCE = 16;
    private final int DONT_REBROADCAST_DISTANCE = 16;
	
	public Guard(RobotController rc){	
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
		this.base = rc.getLocation();
		this.prevHealth = rc.getHealth();
		this.myType = RobotType.SOLDIER;
		this.attackRadius = myType.attackRadiusSquared;
		this.myArchons = rc.getInitialArchonLocations(myTeam);
		this.enemyArchons = rc.getInitialArchonLocations(otherTeam);
		RobotInfo[] friends = rc.senseNearbyRobots(MAX_RANGE, myTeam);
		squadArchon = Utility.getBotOfType(friends, RobotType.ARCHON, rand, rc).ID;
	}
	
	@Override
	public void run() {
		while(true){
			try {
				rc.setIndicatorString(0, ""+squadArchon);
				if (base != null) {
					rc.setIndicatorString(1, "Base X: "+base.x+" Y: "+base.y);
				}
				//TODO Refactor to compute these only if necessary
				RobotInfo[] enemiesSeen = rc.senseHostileRobots(rc.getLocation(), MAX_RANGE);
				RobotInfo[] friendsSeen = rc.senseNearbyRobots(MAX_RANGE, myTeam);
				RobotInfo[] enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), attackRadius);

				//Change some flags if necessary
				beingAttacked = (rc.getHealth() < prevHealth);
				beingSniped = (enemiesSeen.length == 0 && beingAttacked);
				
				//Attack code
				RobotInfo targetEnemy = null;
				if(enemiesWithinRange.length > 0) { //We're in combat
					targetEnemy = Utility.getTarget(enemiesWithinRange, 0, rc.getLocation());
					if( targetEnemy != null && rc.isWeaponReady() && rc.canAttackLocation(targetEnemy.location)) {
						rc.attackLocation(targetEnemy.location);
					}
				}			

				Utility.cyanidePill(rc);
				
				//Amortizes bytecode usage
				if (!rc.isCoreReady()) {
					handleMessages();
				} else {
					//Movement code
					Direction dirToGo = Direction.NONE;
				    if (enemiesSeen.length > 0) {
				    	//TODO Optimize. Implement switch to just stay out of range while followed, else stay just out of range.
				    	//Can probably implement by having them stand still if enemy still, else run along at rate of enemy
				    	MapLocation myLocation = rc.getLocation();
				    	if(targetEnemy == null) targetEnemy = Utility.getTarget(enemiesSeen, 0, rc.getLocation());
						Direction dirToTarget = myLocation.directionTo(targetEnemy.location);
						int distanceToTarget = myLocation.distanceSquaredTo(targetEnemy.location);
						int kitedDistanceToTarget = myLocation.add(dirToTarget.opposite()).distanceSquaredTo(targetEnemy.location);
						//Currently, this keeps them just in range
						if (kitedDistanceToTarget <= (RobotType.SOLDIER.attackRadiusSquared) && targetEnemy.type != RobotType.ZOMBIEDEN && !protectingBase && !beingSniped) {
							//KIIITTTEEEE
							dirToGo = dirToTarget.opposite();
							prevDirection=Utility.tryToMove(rc, dirToGo, prevDirection);
							
						} else if (targetEnemy.type == RobotType.ZOMBIEDEN && distanceToTarget < 10) {
							//Do nothing
							
						} else if (distanceToTarget > RobotType.SOLDIER.attackRadiusSquared){
							//Get closer if we can't hit them
							prevDirection=Utility.tryToMove(rc, dirToTarget,prevDirection);
							dirToGo = dirToTarget;
						}
					
				    } else if (rc.getHealth() / myType.maxHealth < RETREAT_HEALTH_PERCENT) {
				    	//Retreat
				    	//TODO Optimize
							if (Utility.chance(rand, .7)) {
								dirToGo = rc.getLocation().directionTo(base);
							} else if (enemiesWithinRange.length > 0) {
								dirToGo = rc.getLocation().directionTo(enemiesWithinRange[0].location).opposite();
							}
							prevDirection=Utility.tryToMove(rc, dirToGo, prevDirection);	
							
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
						
					} else if (friendsSeen.length > 0) {
						
						dirToGo = swarmMovement();
						
					}
							
					if (rc.isWeaponReady()) {
						enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), RobotType.SOLDIER.attackRadiusSquared);
						if(enemiesWithinRange.length > 0) { //We're in combat
							targetEnemy = Utility.getTarget(enemiesWithinRange, 0, rc.getLocation());
							if( targetEnemy != null && rc.canAttackLocation(targetEnemy.location)) {
								rc.attackLocation(targetEnemy.location);
							}
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
				rc.setIndicatorString(2, squadArchon + "should sometimes equal " + id);
				if (id == squadArchon) {
					base = message.getLocation(); //Update squad archon location
				}
				//TODO Include ignore bit to lower melee overhead
				if(contents != null) { //Not a basic signal
					int code = Comms.getMessageCode(contents[0]);
					int aux = Comms.getAux(contents[0]);
					MapLocation loc;
					switch (code){
						case Comms.ATTACK_DEN:
							loc = Comms.decodeLocation(contents[1]);
							currentOrderedGoal = loc;
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
			if (Utility.chance(rand, .8)) {
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
			if (Utility.chance(rand, .8)) {
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
