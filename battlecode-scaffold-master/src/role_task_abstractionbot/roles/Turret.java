package role_task_abstractionbot.roles;

import battlecode.common.Clock;
import battlecode.common.RobotController;
import role_task_abstractionbot.Role;
import role_task_abstractionbot.Task;
import role_task_abstractionbot.tasks.TurretDerp;

public class Turret implements Role {
	private RobotController rc;
	Task next;
	
	public Turret(RobotController rc) {
		this.rc = rc;
		next = new TurretDerp(rc);
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
