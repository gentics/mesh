package com.gentics.mesh.distributed.coordinator;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class Coordinator {

	private static final Logger log = LoggerFactory.getLogger(Coordinator.class);

	@Inject
	public Coordinator() {
	}

	public MasterServer getElectedMaster() {
		return null;
	}

}
