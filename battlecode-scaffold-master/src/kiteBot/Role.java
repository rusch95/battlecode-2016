package kiteBot;

public interface Role {

	/**
	 * Will be run exactly once. Do not let this escape or the main run() will also escape and the robot will explode.
	 */
	public void run();
}
