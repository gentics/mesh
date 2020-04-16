package com.gentics.mesh.server.cluster.test.task;

public interface LoadTask {

	void runTask(long txDelay, boolean lockTx, boolean lockForDBSync);

}
