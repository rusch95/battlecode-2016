package role_task_abstractionbot2.roles;

import battlecode.common.Clock;
import battlecode.common.RobotController;
import role_task_abstractionbot2.Role;
import role_task_abstractionbot2.Task;
import role_task_abstractionbot2.tasks.NonTurretDerp;

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
