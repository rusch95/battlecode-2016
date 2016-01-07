package abstractionexamplebot;

import battlecode.common.*;

import java.util.Random;



/**
 * Tests indicate that this abstraction has a small overhead (~10bytecodes/300 total, but it might scale with increased calculation size)
 * @author Ryan
 *
 */
public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        // You can instantiate variables here.
        RobotHandler me = null;

        if (rc.getType() == RobotType.ARCHON) {
            try {
                // Any code here gets executed exactly once at the beginning of the game.
            	me = new Archon(rc);
            } catch (Exception e) {
                // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
                // Caught exceptions will result in a bytecode penalty.a
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
        } else { //It's a turret
            try {
                me = new Turret(rc);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
        
        //run the designated RobotHandler
        while (true) {
        	try {
        		me.run();
        		Clock.yield();
        	} catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
        
    }
}
