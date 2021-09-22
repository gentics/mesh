package com.gentics.mesh.monitor.liveness;

/**
 * Liveness manager interface
 */
public interface LivenessManager {

	/**
	 * Check whether Mesh is live
	 * @return true for live
	 */
	boolean isLive();

	/**
	 * Get the error, which was set when Mesh was set to be "dead"
	 * @return error or null
	 */
	String getError();

	/**
	 * Set the liveness flag
	 * @param live flag
	 * @param error error
	 */
	void setLive(boolean live, String error);

	/**
	 * Shutdown the manager
	 */
	void shutdown();
}
