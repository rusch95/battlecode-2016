package ryan_sandbox_bot;

import java.util.Random;

import battlecode.common.*;

public class Archon implements Role {
	private RobotController rc;
	private Random rand;
	private MapLocation partsTarget;
	private final Team myTeam;
    
	public Archon(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		try {
			tryToBuild(RobotType.SCOUT);
		} catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
    	}
	}
	
	@Override
	public void run() {
		while(true){
			try {
				Signal[] messages = rc.emptySignalQueue();
				handleMessages(messages);
				int fate = rand.nextInt(1000);
				if(fate > 500 || rc.getTeamParts() > 50){ //50% chance of not building if we're low on parts, so other Archons can use the parts
					if(rc.isCoreReady()) {
						if(fate > 800)
							tryToBuild(RobotType.SOLDIER);
						else if(fate > 700) tryToBuild(RobotType.SCOUT);
						else tryToBuild(RobotType.TURRET);
					}
				}
				if(partsTarget == null) partsTarget = searchForParts();
				if(partsTarget != null && rc.isCoreReady()) {
					rc.setIndicatorDot(partsTarget, 100, 0, 0);
					Utility.tryToMove(rc, rc.getLocation().directionTo(partsTarget));
				}
				if(partsTarget != null && partsTarget.equals(rc.getLocation())){
					partsTarget = null;
				}
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield();
		}
		
	}
	
	private void handleMessages(Signal[] messages) {
		for(Signal message : messages) {
			if(message.getTeam().equals(myTeam) && message.getMessage()[0] == 45 && partsTarget == null) { //PARTS!
				partsTarget = Utility.decodeLocation(message.getMessage()[1]);
				break;
			}
		}
	}
	
	/**
	 * Tries to build the specified RobotType in any direction.
	 * @param typeToBuild type of robot to build
	 * @throws GameActionException 
	 */
	private void tryToBuild(RobotType typeToBuild) throws GameActionException {
        // Choose a random direction to try to build in
        Direction dirToBuild = Utility.getRandomDirection(rand);
        for (int i = 0; i < 8; i++) {
            // If possible, build in this direction
            if (rc.canBuild(dirToBuild, typeToBuild)) {
                rc.build(dirToBuild, typeToBuild);
                return;
            } else {
                // Rotate the direction to try
                dirToBuild = dirToBuild.rotateLeft();
            }
        }
	}
	
	/**
	 * Detects the largest parts pile in sight.
	 * @return MapLocation of largest parts pile, or null if none found.
	 */
	private MapLocation searchForParts() {
		MapLocation[] nearbyLocations = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), RobotType.ARCHON.sensorRadiusSquared);
		MapLocation bestPartsPile = null;
		double maxParts = 0;
		for(MapLocation location : nearbyLocations) {
			double parts = rc.senseParts(location);
			if(parts > maxParts) {
				bestPartsPile = location;
				maxParts = parts;
			}
		}
		return bestPartsPile;
	}
	
}
