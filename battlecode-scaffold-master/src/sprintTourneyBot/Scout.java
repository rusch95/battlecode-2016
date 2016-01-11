package sprintTourneyBot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Scout implements Role {
	private final RobotController rc;
	private final Random rand;
    private final Team myTeam;
    private final Team otherTeam;
    private final MapLocation birthplace;
    private final HashMap<MapLocation, Integer> theMap;
    private final ArrayList<MapLocation> dens;
    
	public Scout(RobotController rc){
		this.rc = rc;
		this.rand = new Random(rc.getID());
		this.myTeam = rc.getTeam();
		this.otherTeam = myTeam.opponent();
		this.birthplace = rc.getLocation();
		this.theMap = new HashMap<MapLocation, Integer>();
		this.dens = new ArrayList<MapLocation>();
	}
	
	@Override
	public void run() {
		while(true){
			try {
				
				
				
				
				
				
				
			} catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	    	}
			Clock.yield();
		}
		
	}
	//~~~~~~~~~~~~~~~~~~~~END MAIN LOOP~~~~~~~~~~~~~~~~~
	
	/**
	 * Scans the sight range of the Scout
	 * @throws GameActionException
	 */
	private void scan() throws GameActionException {

		
		
	}
}
