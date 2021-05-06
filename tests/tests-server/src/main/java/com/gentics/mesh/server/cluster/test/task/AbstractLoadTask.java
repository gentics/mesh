package com.gentics.mesh.server.cluster.test.task;

import com.gentics.mesh.server.cluster.test.AbstractClusterTest;

/**
 * Test task
 */
public abstract class AbstractLoadTask implements LoadTask {

	protected AbstractClusterTest test;

	public AbstractLoadTask(AbstractClusterTest test) {
		this.test = test;
	}

}
