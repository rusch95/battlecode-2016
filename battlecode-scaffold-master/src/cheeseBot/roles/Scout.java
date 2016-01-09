package cheeseBot.roles;

import battlecode.common.Clock;
import battlecode.common.RobotController;
import cheeseBot.Role;
import cheeseBot.Task;
import cheeseBot.tasks.ScoutDerp;

public class Scout implements Role {
	private RobotController rc;
	Task next;
	
	public Scout(RobotController rc) {
		this.rc = rc;
		next = new ScoutDerp(rc);
	}
	
	@Override
	public void run() {
		int state = 2;
		while(state == 2) {
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
