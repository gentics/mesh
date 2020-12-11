package com.gentics.mesh.server.cluster.test.task;

/**
 * Executable test task which invokes load.
 */
public interface LoadTask {

	void runTask(long txDelay, boolean lockTx, boolean lockForDBSync);

}
