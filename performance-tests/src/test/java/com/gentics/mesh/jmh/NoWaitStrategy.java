package com.gentics.mesh.jmh;

import org.testcontainers.containers.GenericContainer.AbstractWaitStrategy;

public class NoWaitStrategy extends AbstractWaitStrategy {

	@Override
	protected void waitUntilReady() {

	}

}