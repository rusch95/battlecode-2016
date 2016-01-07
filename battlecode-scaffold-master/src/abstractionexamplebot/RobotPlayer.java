package abstractionexamplebot;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        // You can instantiate variables here.
        Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
                Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
        RobotType[] robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
                RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET};
        Random rand = new Random(rc.getID());
        int myAttackRange = 0;
        Team myTeam = rc.getTeam();
        Team enemyTeam = myTeam.opponent();
        RobotHandler me = null;

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
                myAttackRange = rc.getType().attackRadiusSquared;
                me = new NonTurret(rc);
            } catch (Exception e) {
                // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
                // Caught exceptions will result in a bytecode penalty.
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        } else { //It's a turret
            try {
                myAttackRange = rc.getType().attackRadiusSquared;
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
