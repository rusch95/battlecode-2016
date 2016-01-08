package role_task_abstractionbot.roles;

import battlecode.common.Clock;
import battlecode.common.RobotController;
import role_task_abstractionbot.Role;
import role_task_abstractionbot.Task;
import role_task_abstractionbot.tasks.NonTurretDerp;

public class NonTurret implements Role {
	private RobotController rc;
	Task next;
	
	public NonTurret(RobotController rc) {
		this.rc = rc;
		next = new NonTurretDerp(rc);
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
