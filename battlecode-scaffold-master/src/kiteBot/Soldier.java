package kiteBot;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
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

    private Direction prevDirection = Direction.NONE;
    
    //Global Flags
    private boolean protectingBase = false;
    private boolean attackingOtherTeam = false;
    private boolean attackingZombies = false;
    private boolean attackingTurrets = false;
    private boolean attackingDen = false;
    private boolean attackingTurtle = false;
    private boolean actingOnMessage = false;
    
    //Global Locations
    private MapLocation base;
    private MapLocation otherTeamRadioedLoc = null;
    private MapLocation zombieRadioedLoc = null;
    private MapLocation turretRadioedLoc = null;
    private MapLocation denRadioedLoc = null;
    private MapLocation turtleRadioedLoc = null;

    
    //Magic Numbers
    private final int CLOSE_RANGE = 2;
    private final int MED_RANGE = 17;
    private final int FAR_RANGE = 25;
    private final int MAX_RANGE = -1;
	private final int CLOSE_TOO_MANY = 2;
	private final int CLOSE_TOO_FEW = 0;
	private final int MED_TOO_MANY = 10;
	private final int MED_TOO_FEW = 5;
	private final int FAR_TOO_MANY = 999;
	private final int FAR_TOO_FEW = 0;
	private final int MIN_SQUAD_NUM = 1;
	private final double RETREAT_HEALTH_PERCENT = 0.35;
	
	private final int BROADCAST_OTHER_TEAM_ATTACK = 5;
	private final int REBROADCAST_DISTANCE = 300;
	private final int BASIC_GET_HELP_RANGE = 625;
    
	public Soldier(RobotController rc){
		
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
		this.base = rc.getLocation();
	}
	
	@Override
	public void run() {
		while(true){
			try {
				RobotInfo[] enemiesSeen = rc.senseHostileRobots(rc.getLocation(), MAX_RANGE);
				RobotInfo[] friendsSeen = rc.senseNearbyRobots(MAX_RANGE, myTeam);
				RobotInfo[] enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), RobotType.SOLDIER.attackRadiusSquared);
				Signal[] messageQueue = rc.emptySignalQueue();
				
				//Attack code
				RobotInfo targetEnemy = null;
				if(enemiesWithinRange.length > 0 && rc.isWeaponReady()) { //We're in combat
					targetEnemy = Utility.getTarget(enemiesWithinRange, 0, rc.getLocation());
					if( targetEnemy != null) {
						rc.attackLocation(targetEnemy.location);
					}
				}
				
				//Broadcasting code
				if (!attackingOtherTeam && targetEnemy != null) {
					if (enemiesSeen.length > BROADCAST_OTHER_TEAM_ATTACK && targetEnemy.team == otherTeam) {
						Comms.broadcastBasic(Comms.BASIC_FOUND_OTHER_TEAM, BASIC_GET_HELP_RANGE, rc);
						otherTeamRadioedLoc = rc.getLocation();
						attackingOtherTeam = true;
					}
				} else if (!attackingDen && targetEnemy != null) {
					if (targetEnemy.type == RobotType.ZOMBIEDEN) {
						Comms.broadcastBasic(Comms.BASIC_FOUND_DEN, BASIC_GET_HELP_RANGE, rc);
						denRadioedLoc = rc.getLocation();
						attackingDen = true;
					}				
				} else if (!attackingZombies && targetEnemy != null) {
					if (enemiesSeen.length > BROADCAST_OTHER_TEAM_ATTACK && targetEnemy.team == Team.ZOMBIE) {
						Comms.broadcastBasic(Comms.BASIC_FOUND_OTHER_TEAM, BASIC_GET_HELP_RANGE, rc);
						zombieRadioedLoc = rc.getLocation();
						attackingZombies = true;
					}
				}
				
				//Rebroadcast code
				if (attackingOtherTeam) {
					if (rc.getLocation().distanceSquaredTo(otherTeamRadioedLoc) > REBROADCAST_DISTANCE) {
						//Clean slate to recalculate location of enemy
						Comms.broadcastBasic(Comms.BASIC_ATTACK_FINISHED, BASIC_GET_HELP_RANGE, rc);
					}
				}
				if (attackingZombies) {
					if (rc.getLocation().distanceSquaredTo(zombieRadioedLoc) > REBROADCAST_DISTANCE) {
						//Clean slate to recalculate location of enemy
						Comms.broadcastBasic(Comms.BASIC_ATTACK_FINISHED, BASIC_GET_HELP_RANGE, rc);
					}
				}
				
				//Done attacking code
				if (enemiesSeen.length == 0) {
					if (attackingDen) {
						Comms.broadcastBasic(Comms.BASIC_ATTACK_FINISHED, BASIC_GET_HELP_RANGE, rc);
					} else if (attackingZombies || attackingOtherTeam) {
						Comms.broadcastBasic(Comms.BASIC_ATTACK_FINISHED, BASIC_GET_HELP_RANGE, rc);
					}
				}
				
				 //Flee code
			    if (rc.getHealth() /rc.getType().maxHealth < RETREAT_HEALTH_PERCENT) {	    	
					Direction dirToGo = Direction.NONE;
						if (Utility.chance(rand, .7)) {
							dirToGo = rc.getLocation().directionTo(base);
						} else if (Utility.chance(rand, .7) && enemiesWithinRange.length > 0) {
							dirToGo = rc.getLocation().directionTo(enemiesWithinRange[0].location).opposite();
						}
						prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);		
				
						
						
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
					
				} else if (friendsSeen.length > 0) {
					
					RobotInfo[] closeFriends = rc.senseNearbyRobots(CLOSE_RANGE, myTeam); //Magic number
					RobotInfo[] medFriends = rc.senseNearbyRobots(MED_RANGE, myTeam); //More magic
					
					RobotInfo weakFriend = Utility.getWeakest(friendsSeen);
					
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
					
					//To Handle if they moved into a range where they can now shoot the enemy
					if (rc.isWeaponReady()) {
						enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), RobotType.SOLDIER.attackRadiusSquared);
						if(enemiesWithinRange.length > 0) { //We're in combat
							targetEnemy = Utility.getTarget(enemiesWithinRange, 0, rc.getLocation());
							if( targetEnemy != null) {
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
	
	
	
}
