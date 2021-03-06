package mainBot;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;

public class Turret implements Role {
	private final RobotController rc;
	private final Random rand;
    private final Team myTeam;
    private final Team otherTeam;
    private Direction prevDirection = Direction.NONE;
    
    private MapLocation targetEnemy;
    private boolean targetUpdated;
    
    private int minX = 0;
    private int maxX = Integer.MAX_VALUE;
    private int minY = 0;
    private int maxY = Integer.MAX_VALUE;
    
    private boolean minXFound = false;
    private boolean maxXFound = false;
    private boolean minYFound = false;
    private boolean maxYFound = false;
    
    //Constants
    private static final int NEED_RECON_RANGE = 8;
    
	public Turret(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
	}
	
	@Override
	public void run() {
		while(true){
			while(rc.getType() == RobotType.TURRET) {
				try {
					targetUpdated = false;
					handleMessages();
					if(targetEnemy != null) rc.setIndicatorDot(targetEnemy, 250, 0, 250);
					attack();
					checkForRecon();
					if (Utility.chance(rand, .5)) {
						RobotInfo[] adjFriends = rc.senseNearbyRobots(9, myTeam);
						if (Utility.getNumberOfBotOfType(adjFriends, RobotType.TURRET ) > 5) {
							rc.pack();
						} else {
							//TODO Fix getting unit stuck
							RobotInfo[] nearby = rc.senseNearbyRobots(2, rc.getTeam());
							if (nearby.length > 2 && rc.getTeamParts() > 400 && Utility.chance(rand, .33)) {
								rc.pack();
							}
						}
					}
				} catch (Exception e) {
		            System.out.println(e.getMessage());
		            e.printStackTrace();
		    	}
				Clock.yield();
			}
			while(rc.getType() == RobotType.TTM) {
				try {
					RobotInfo[] adjFriends = rc.senseNearbyRobots(9, myTeam);
					RobotInfo archon = Utility.getBotOfType(adjFriends, RobotType.ARCHON, rand, rc);
					Direction dirToGo = rc.getLocation().directionTo(archon.location).opposite();
					prevDirection=Utility.tryToMove(rc, dirToGo,prevDirection);
					if (Utility.chance(rand, .5)) {
						if (Utility.getNumberOfBotOfType(adjFriends, RobotType.TURRET ) <= 4) {
							rc.unpack();
						}
					}		 
				} catch (Exception e)  {
		            System.out.println(e.getMessage());
		            e.printStackTrace();
		    	}
				Clock.yield();
			}
		}
	}
	//~~~~~~~~~~~~~~~~~~END MAIN LOOP~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Handles the contents of the signal queue
	 */
	public void handleMessages() {
		Signal[] messages = rc.emptySignalQueue();
		for(Signal message : messages) {
			if(message.getTeam().equals(myTeam)){ //Friendly message
				int[] contents = message.getMessage();
				if(contents != null) { //Not a basic signal
					int id = message.getID();
					int code = Comms.getMessageCode(contents[0]);
					int aux = Comms.getAux(contents[0]);
					MapLocation loc = Comms.decodeLocation(contents[1]);
					switch (code){
						case Comms.TURRET_ATTACK_HERE:
								if(loc.distanceSquaredTo(rc.getLocation()) <= RobotType.TURRET.attackRadiusSquared) {
									targetEnemy = loc;
									targetUpdated = true;
								}
							break;
						case Comms.FOUND_MINX:
							if(!minXFound) {
								minX = aux;
								minXFound = true;
							}
							break;
						case Comms.FOUND_MAXX:
							if(!maxXFound) {
								maxX = aux;
								maxXFound = true;
							}
							break;
						case Comms.FOUND_MINY:
							if(!minYFound) {
								minY = aux;
								minYFound = true;
							}
							break;
						case Comms.FOUND_MAXY:
							if(!maxYFound) {
								maxY = aux;
								maxYFound = true;
							}
							break;
					}
				}
			}
		}
	}
	
	/**
	 * Deals damage to optimal target (if possible)
	 * @throws GameActionException
	 */
	public void attack() throws GameActionException {
		if(targetUpdated && rc.isWeaponReady() && rc.canAttackLocation(targetEnemy)) { //Snipe sighted
			rc.attackLocation(targetEnemy);
		}
		else {
			RobotInfo[] enemiesWithinRange = rc.senseHostileRobots(rc.getLocation(), RobotType.TURRET.attackRadiusSquared);
			if(enemiesWithinRange.length > 0) { //We're in combat
				RobotInfo nearTargetEnemy = Utility.getTarget(enemiesWithinRange, GameConstants.TURRET_MINIMUM_RANGE, rc.getLocation());
				if(rc.isWeaponReady() && nearTargetEnemy != null) {
					rc.attackLocation(nearTargetEnemy.location);
				}
			}
		}
	}
	
	/**
	 * Checks for available recon, and requests if necessary.
	 * @throws GameActionException 
	 */
	public void checkForRecon() throws GameActionException {
		RobotInfo[] nearbyFriendlies = rc.senseNearbyRobots(NEED_RECON_RANGE, myTeam);
		boolean weGood = false;
		for(RobotInfo friendly : nearbyFriendlies) {
			if(friendly.type.equals(RobotType.SCOUT)) {
				weGood = true;
				break;
			}
		}
		if(!weGood && rc.getRoundNum()%3 == 1) {
			rc.broadcastSignal(25);
		}
	}
	
}
