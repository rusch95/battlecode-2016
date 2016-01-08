package role_task_abstractionbot;

import battlecode.common.*;

import role_task_abstractionbot.roles.*;

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
    	 */
    	
        // You can instantiate variables here.
    	Role me = null;
    	
        if (rc.getType() == RobotType.ARCHON) {
            try {
                // Any code here gets executed exactly once at the beginning of the game.
            	me = new Archon(rc);
            } catch (Exception e) {
                // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
                // Caught exceptions will result in a bytecode penalty.
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        } else if (rc.getType() != RobotType.TURRET) {
            try {
                // Any code here gets executed exactly once at the beginning of the game.
            	me = new NonTurret(rc);
            } catch (Exception e) {
                // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
                // Caught exceptions will result in a bytecode penalty.
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        } else { //Is a turret
            try {
            	me = new Turret(rc);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
        
        //Run the Role, ONCE.
        try {
        	me.run();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        
    }
}
