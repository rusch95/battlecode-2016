package cheeseBot.roles;

import cheeseBot.*;
import cheeseBot.tasks.*;
import battlecode.common.*;

public class Archon implements Role {
	private RobotController rc;
	private Task next;
	
	public Archon(RobotController rc) {
		this.rc = rc;
		this.next = new ArchonDerp(rc);
	}
	
	@Override
	public void run() {
		int state = 0;
		while(state == 0) {
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
