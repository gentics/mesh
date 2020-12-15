package com.gentics.mesh.server.cluster.test.task;

/**
 * Executable test task which invokes load.
 */
public interface LoadTask {

	/**
	 * Run the task
	 * 
	 * @param txDelay
	 *            Additional delay for the transaction (e.g. to reduce load)
	 * @param lockTx
	 *            Whether to use locks
	 * @param lockForDBSync
	 *            Whether to lock during db sync events
	 */
	void runTask(long txDelay, boolean lockTx, boolean lockForDBSync);

}
