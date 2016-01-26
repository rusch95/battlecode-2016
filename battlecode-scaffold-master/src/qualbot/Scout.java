package qualbot;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Signal;

public class Scout extends Role {
	private boolean providingBackup = false; //Overrides the current state
	private int needsBackup;
	private MapLocation backupFlag;
	private MapLocation objectiveLocation; //Current objective for the robot to move to
	private int distanceToStayBackFromObjective = 0; //This controls how close they get to an objective, such as one step away from dens
	
	//Robots Seen
	private RobotInfo[] enemiesInSight;
	private RobotInfo[] enemiesInRange;
	private RobotInfo[] friendsInSight;
	
	//Constants for gotoObjective
	private static final int tooFarAwayThreshold = 30;  //Don't consider friends distances greater than this from us
	private static final int closerToGoalThreshold = 6; //Go towards friend if closer to goal by this amount
	
	//To be sorted
	private short[] mapLongitudesVisited = new short[280]; //Divide the map into 5 width lane
	
	public Scout(RobotController rc) {
		super(rc);
		
		//TEST CODE
		this.backupFlag = archonThatSpawnedMe.location;
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

				if(enemiesInRange.length > 0) {
						kite();
					} else {
						gotoObjective();
					}				
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield(); //TODO: Move this? Exceptions waste a round this way
		}
	}

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
	
	/*
	 * Goes to objectiveLocation up to distance distanceToStayBackFromObjective
	 */
	private void gotoObjective() throws GameActionException{
		if(rc.isCoreReady() && objectiveLocation != null) {
			Direction dirToObjective =  myLocation.directionTo(objectiveLocation);
		
			//First let's see if we can move straight towards the objective
			for (int deltaD:forwardDirectionsToTry) {
				//TODO Could slightly optimize by choosing diagonal direction of most friends first
				Direction attemptDirection = Direction.values()[(dirToObjective.ordinal()+deltaD+8)%8];
				if(rc.canMove(attemptDirection)) {
					rc.move(attemptDirection);
					return;
				}
			}
		}
	}
}
