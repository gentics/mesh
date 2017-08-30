package com.gentics.mesh.distributed.containers;

import org.testcontainers.containers.GenericContainer.AbstractWaitStrategy;

public class NoWaitStrategy extends AbstractWaitStrategy {

	@Override
	protected void waitUntilReady() {

	}

}
