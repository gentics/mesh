package com.gentics.mesh.test.docker;

import org.testcontainers.containers.GenericContainer.AbstractWaitStrategy;

public class NoWaitStrategy extends AbstractWaitStrategy {

	@Override
	protected void waitUntilReady() {

	}

}
