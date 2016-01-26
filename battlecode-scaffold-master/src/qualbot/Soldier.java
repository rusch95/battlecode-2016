package qualbot;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Soldier extends Role {
	//Robots Seen
	private RobotInfo[] enemiesInSight;
	private RobotInfo[] enemiesInRange;
	private RobotInfo[] friendsInSight;
	
	//Misc State fields
	private boolean providingBackup = false; //Overrides the current state
	private int needsBackup;
	private MapLocation backupFlag;
	private int objectiveMargin = 0; //This controls how close they get to an objective, such as one step away from dens
	private boolean atObjective; //True if the robot has entered the required margin for the objective
	private RobotInfo targetEnemy;
	
	public Soldier(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		while(true) {
			try {
				myLocation = rc.getLocation();
				targetEnemy = null;
				enemiesInSight = rc.senseHostileRobots(myLocation, -1);
				enemiesInRange = rc.senseHostileRobots(myLocation, attackRadiusSquared);
				friendsInSight = rc.senseNearbyRobots(-1, myTeam);
				handleMessages(); //TODO Possibly amortize to every other turn if bytecode gets too high

				if(enemiesInRange.length > 0) {
					targetEnemy = getAttackTarget(enemiesInRange, minRange, myLocation);
					dealDamage();
					kite(targetEnemy);
				} else if(enemiesInSight.length > 0) {
					targetEnemy = getAttackTarget(enemiesInSight, minRange, myLocation);
					kite(targetEnemy);
				} else if(providingBackup) { //supercedes the current state
					gotoObjective(backupFlag, objectiveMargin, objectiveMargin+15, friendsInSight);
				} else { //execute the current state
					gotoObjective(objectiveFlag, objectiveMargin, objectiveMargin+15, friendsInSight);
				}
				targetEnemy = getAttackTarget(enemiesInRange, minRange, myLocation);
				dealDamage();
				
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield(); //TODO: Move this? Exceptions waste a round this way
		}
	}

	/**
	 * Kite-moves away from a given enemy
	 * @param kitingTarget
	 * @throws GameActionException
	 */
	private void kite(RobotInfo kitingTarget) throws GameActionException {
		Direction toTarget = myLocation.directionTo(kitingTarget.location);
		int currentDistance = myLocation.distanceSquaredTo(kitingTarget.location);
		int kiteDistance = myLocation.add(toTarget.opposite()).distanceSquaredTo(kitingTarget.location);
		
		if(kiteDistance <= attackRadiusSquared && targetEnemy.type != RobotType.ZOMBIEDEN) {
			tryToMove(toTarget.opposite());
		} else if (currentDistance > attackRadiusSquared) {
			tryToMove(toTarget);
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
							objectiveFlag = loc;
							state = SIEGING_DEN;
						}
						break;
					case Comms.DEN_DESTROYED:
						if(state == SIEGING_DEN && objectiveFlag.equals(loc)) {
							state = IDLE;
						}
						break;
					case Comms.ATTACK_ENEMY:
						if(state == IDLE) {
							objectiveFlag = loc;
							state = SIEGING_ENEMY;
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
	
	private void dealDamage() throws GameActionException {
		if(rc.isWeaponReady()) {
			if(targetEnemy != null && rc.canAttackLocation(targetEnemy.location)) {
				rc.attackLocation(targetEnemy.location);
			} else {
				rc.setIndicatorString(0, "FAILED TO ATTACK");
			}
		}
	}
	
	private static final int[] forwardDirectionsToTry = {0, 1, -1};
	private static final int[] secondaryDirectionsToTry = {2, -2, 3, -3};
	private static final int[] allTheDirections = {0, 1, -1, 2, -2, 3, -3};
	
	/**
	 * Goes to the location specified, and stays a certain distance away from it.
	 * @param flag objective location
	 * @param hysterisis margin required to initially be at the objective
	 * @param margin of distance that is satisfactory to be away from the objective
	 * @throws GameActionException
	 */
	private void gotoObjective(MapLocation flag, int hysterisis, int margin) throws GameActionException{
		if(rc.isCoreReady() && flag != null) {
			Direction dirToObjective =  myLocation.directionTo(flag);
			int distanceToObjective = myLocation.distanceSquaredTo(flag);
			if ( (distanceToObjective > hysterisis && !atObjective)	|| distanceToObjective > margin) {
				atObjective = distanceToObjective <= margin;
				//First let's see if we can move straight towards the objective
				for (int deltaD:forwardDirectionsToTry) {
					//TODO Could slightly optimize by choosing diagonal direction of most friends first
					Direction attemptDirection = Direction.values()[(dirToObjective.ordinal()+deltaD+8)%8];
					if(rc.canMove(attemptDirection)) {
						rc.move(attemptDirection);
						return;
					}
				}		
				//Move torwards some friend if they are closer to the goal than us
				RobotInfo friendCloserToGoal = null;
				for(RobotInfo robot:friendsInSight){
					//First let's find a friend that fits our profile
					int robotDistanceToGoal = robot.location.distanceSquaredTo(flag);
					if ((robotDistanceToGoal - closerToGoalThreshold) > distanceToObjective && myLocation.distanceSquaredTo(robot.location) < tooFarAwayThreshold) {			
						friendCloserToGoal = robot;
						break;
					}
				}
				//TODO Replace with a better movement towards friend, such as sideways in the friends direction
				if (friendCloserToGoal != null) {
					//And then let's move towards that friend
					Direction dirToFriend =  myLocation.directionTo(flag);
					for (int deltaD:forwardDirectionsToTry) {
						Direction attemptDirection = Direction.values()[(dirToFriend.ordinal()+deltaD+8)%8];
						if(rc.canMove(attemptDirection)) {
							rc.move(attemptDirection);
							return;
						}
					}	
				}
				
				//Then, we dig through the rubble
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
				
				//Finally, we try moving sideways or backwards
				for (int deltaD:secondaryDirectionsToTry) {
					Direction attemptDirection = Direction.values()[(dirToObjective.ordinal()+deltaD+8)%8];
					if(rc.canMove(attemptDirection)) {
						rc.move(attemptDirection);
						return;
					}
				}
			}
		}
		else atObjective = true;
	}
}
