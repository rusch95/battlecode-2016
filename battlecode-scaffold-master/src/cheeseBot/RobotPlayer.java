package cheeseBot;

import battlecode.common.RobotController;
import battlecode.common.RobotType;
import cheeseBot.roles.Archon;
import cheeseBot.roles.NonTurret;
import cheeseBot.roles.Turret;
import cheeseBot.roles.Scout;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
	
    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
    	/**
    	 * RobotPlayer in this abstraction model goes through enough rounds to determine a bot's Role,
    	 * and then calls Role.run() ONCE. That run() should not escape, so the the robot continues running in it's Role.
    	 * YOU MUST MUST MUST CATCH GameActionExceptions INSIDE of your loops, or they will break out.
    	 * 
    	 * Bytecode overhead looks to be about 40-50, with no information on whether or not it scales poorly
    	 * 
    	 * TODO: Parallelization of tasks?? ADT? Maybe unnecessary. Maybe just have subtasks or just helper methods.
    	 * 
    	 * CHANGE: Only one try/catch loop, since if exception, the robot isn't initializing anyways.
    	 */
    	
        // You can instantiate variables here.
    	Role me = null;
    	try {
    		if (rc.getType() == RobotType.ARCHON) {
    			me = new Archon(rc);
	        } else if (rc.getType() == RobotType.TURRET) {
	        	me = new Turret(rc);
	        } else if (rc.getType() == RobotType.SCOUT) {
	        	me = new Scout(rc);
    		}else { //Is a turret
	        	me = new NonTurret(rc);
	        }
	        //Run the Role, ONCE
	        me.run();
	        
    	} catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
    	}
    }
}
