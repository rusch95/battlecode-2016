package qualbot;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Signal;

public class Soldier extends Role {
	private boolean providingBackup = false; //Overrides the current state
	private int needsBackup;
	private MapLocation backupFlag;
	private MapLocation objectiveLocation; //Current objective for the robot to move to
	
	//Robots Seen
	private RobotInfo[] enemiesInSight;
	private RobotInfo[] enemiesInRange;
	private RobotInfo[] friendsInSight;
	
	public Soldier(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		while(true) {
			try {
				handleMessages(); //TODO Possibly amortize to every other turn if bytecode gets too high
				myLocation = rc.getLocation();
				enemiesInSight = rc.senseHostileRobots(myLocation, -1);
				enemiesInRange = rc.senseHostileRobots(myLocation, attackRadiusSquared);
				friendsInSight = rc.senseNearbyRobots(-1, myTeam);
				

				if (rc.getRoundNum() > 600)
					objectiveLocation = friendlyArchonStartPositions[0];
				if (rc.getRoundNum() > 1000)
					objectiveLocation = friendlyArchonStartPositions[1];
				if (rc.getRoundNum() > 1500)
					objectiveLocation = friendlyArchonStartPositions[2];
				if (rc.getRoundNum() > 1800)
					objectiveLocation = enemyArchonStartPositions[0];
				if (rc.getRoundNum() > 1950)
					objectiveLocation = enemyArchonStartPositions[1];
				if (rc.getRoundNum() > 2100)
					objectiveLocation = enemyArchonStartPositions[2];
				//TEST CODE
				
				if(providingBackup) { //supercedes the current state
					if(enemiesInRange.length > 0) {
						kite();
					} else {
						//move towards backupFlag
					}
				} else { //execute the current state
					if(enemiesInRange.length > 0) {
						kite();
					} else {
						gotoObjective();
					}
				}
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield(); //TODO: Move this? Exceptions waste a round this way
		}
	}

	@Override
	protected void handleMessage(Signal message) {
		if(message.getTeam().equals(myTeam)) {
			int[] contents = message.getMessage();
			if(contents != null) { //Advanced message
				int code = Comms.getMessageCode(contents[0]);
				int aux = Comms.getAux(contents[0]);
				MapLocation loc = Comms.decodeLocation(contents[1]);
				switch (code){
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
					case Comms.ATTACK_DEN:
						if(state == IDLE) {
							targetFlag = loc;
							state = SEIGING_DEN;
						}
						break;
					case Comms.DEN_DESTROYED:
						if(state == SEIGING_DEN && targetFlag.equals(loc)) {
							state = IDLE;
						}
						break;
					case Comms.ATTACK_ENEMY:
						if(state == IDLE) {
							targetFlag = loc;
							state = SEIGING_ENEMY;
						}
						break;
					case Comms.NEED_BACKUP:
						providingBackup = true;
						backupFlag = loc;
						needsBackup = message.getID();
						break;
					case Comms.NO_LONGER_NEED_BACKUP:
						if(providingBackup && message.getID() == needsBackup) {
							providingBackup = false;
						}
						break;
				}
			}
			else { //Basic message
				
			}
		}
	}

	/**
	 * Attack a target, and then move to avoid enemies.
	 * @throws GameActionException 
	 */
	private void kite() throws GameActionException{
		if(rc.isWeaponReady()) {
			RobotInfo target = getAttackTarget(enemiesInRange, minRange, myLocation);
			if(rc.canAttackLocation(target.location)) {
				rc.attackLocation(target.location);
			} else {
				rc.setIndicatorString(0, "FAILED TO ATTACK");
			}
		}
		dodgeEnemies();
	}
	
	
	private static final int[] forwardDirectionsToTry = {0, 1, -1};
	private static final int[] secondaryDirectionsToTry = {2, -2, 3, -3};
	private static final int[] allTheDirections = {0, 1, -1, 2, -2, 3, -3};
	
	private void gotoObjective() throws GameActionException{
		if(rc.isCoreReady() && objectiveLocation != null) {
			Direction dirToObjective =  myLocation.directionTo(objectiveLocation);
			//First let's see if we can move easily towards the objective
			for (int deltaD:forwardDirectionsToTry) {
				//Change to choose side direction of friend close to goal
				Direction attemptDirection = Direction.values()[(dirToObjective.ordinal()+deltaD+8)%8];
				if(rc.canMove(attemptDirection)) {
					rc.move(attemptDirection);
					return;
				}
			}		
			//Move torwards friends if they are closer to the goal
			RobotInfo friendCloserToGoal = null;
			int tooFarAwayThreshold = 30;  //Magic
			int closerToGoalThreshold = 6; //Magic
			int distanceToGoal = myLocation.distanceSquaredTo(objectiveLocation);
			for(RobotInfo robot:friendsInSight){
				int robotDistanceToGoal = robot.location.distanceSquaredTo(objectiveLocation);
				if ((robotDistanceToGoal - closerToGoalThreshold) > distanceToGoal &&
					myLocation.distanceSquaredTo(robot.location) < tooFarAwayThreshold) {
					
					friendCloserToGoal = robot;
					break;
				}
			}
			//TODO Replace this with a better system of going in friend direction
			if (friendCloserToGoal != null) {
				Direction dirToFriend =  myLocation.directionTo(objectiveLocation);
				for (int deltaD:forwardDirectionsToTry) {
					Direction attemptDirection = Direction.values()[(dirToFriend.ordinal()+deltaD+8)%8];
					if(rc.canMove(attemptDirection)) {
						rc.move(attemptDirection);
						return;
					}
				}	
			}
			
			//Finally, we dig through the rubble
			Direction minRubbleDirection = Direction.NONE;
			double minRubble = Double.MAX_VALUE;
			for (int deltaD:forwardDirectionsToTry) {
				Direction attemptDirection = Direction.values()[(dirToObjective.ordinal()+deltaD+8)%8];
				double rubbleAmount = rc.senseRubble(myLocation.add(attemptDirection));
				//Find min rubble in forward direction and dig that
				if (rubbleAmount < minRubble && rubbleAmount > 0) {
					minRubble = rubbleAmount;
					minRubbleDirection = attemptDirection;
				}
			}
			if (minRubbleDirection != Direction.NONE ) {
				rc.clearRubble(minRubbleDirection);
				return;
			}
			for (int deltaD:secondaryDirectionsToTry) {
				//Change to choose side direction of friend close to goal
				Direction attemptDirection = Direction.values()[(dirToObjective.ordinal()+deltaD+8)%8];
				if(rc.canMove(attemptDirection)) {
					rc.move(attemptDirection);
					return;
				}
			}
		}
	}
}
