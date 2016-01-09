package role_task_abstractionbot2.tasks;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import role_task_abstractionbot2.Task;

public class ScoutDerp implements Task {
    Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    private Random rand;
    private Team myTeam;
    private Team enemyTeam;
    private RobotController rc;
    private int sightRange;
    static int[] tryDirections = {0,-1,1,-2,2}; 
	
	public ScoutDerp(RobotController rc) {
		this.rc = rc;
    	this.rand = new Random(rc.getID());
    	this.myTeam = rc.getTeam();
    	this.enemyTeam = myTeam.opponent();
    	this.sightRange = RobotType.SCOUT.sensorRadiusSquared;
	}
	
	public static int getNumberOfBotOfType(RobotInfo[] nearbyRobots, RobotType botType) {
		int numberOf = 0;
		for (RobotInfo bot : nearbyRobots) {
			if (bot.type == botType) {
				numberOf += 1;
			}
		}
		return numberOf;
	}
	
	public void tryToMove(Direction forward) throws GameActionException{
		if(rc.isCoreReady()){
			for(int deltaD:tryDirections){
				Direction maybeForward = Direction.values()[(forward.ordinal()+deltaD+8)%8];
				if(rc.canMove(maybeForward)){
					rc.move(maybeForward);
					return;
				}
			}
			if(rc.getType().canClearRubble()){
				//failed to move, look to clear rubble
				MapLocation ahead = rc.getLocation().add(forward);
				if(rc.senseRubble(ahead)>=GameConstants.RUBBLE_OBSTRUCTION_THRESH){
					rc.clearRubble(forward);
				}
			}
		}
	}
	

	@Override
	public int run() throws GameActionException {
		int fate = rand.nextInt(1000);

        Direction dirToMove = Direction.NONE;

        // If this robot type can attack, check for enemies within range and attack one

        final MapLocation[] sensedLocations = rc.getLocation().getAllMapLocationsWithinRadiusSq(rc.getLocation(), sightRange);
        MapLocation partsLocation = null;
    	for (MapLocation location : sensedLocations) {
    		if (rc.senseParts(location) > 0) {
    			partsLocation = location;
    		}
    	}

        if (true) {
        	if (rc.isCoreReady()) {
                // Choose a random direction to try to move in
        		if (dirToMove == Direction.NONE)
        			dirToMove = directions[fate % 8];
                // Check the rubble in that direction
        		tryToMove(dirToMove);
            }
        }
        return 2;
	}

}
