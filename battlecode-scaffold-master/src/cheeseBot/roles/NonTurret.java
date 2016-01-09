package cheeseBot.roles;

import battlecode.common.Clock;
import battlecode.common.RobotController;
import cheeseBot.Role;
import cheeseBot.Task;
import cheeseBot.tasks.NonTurretDerp;

public class NonTurret implements Role {
	private RobotController rc;
	Task next;
	
	public NonTurret(RobotController rc) {
		this.rc = rc;
		next = new NonTurretDerp(rc);
	}
	
	@Override
	public void run() {
		int state = 1;
		while(state == 1) {
	    	try {
	        	state = next.run();
	        	Clock.yield();
	        } catch (Exception e) {
	        	System.out.println(e.getMessage());
	        	e.printStackTrace();
	        }
		}
	}

}
