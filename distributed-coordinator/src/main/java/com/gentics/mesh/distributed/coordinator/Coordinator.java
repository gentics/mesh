package com.gentics.mesh.distributed.coordinator;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class Coordinator {

	private static final Logger log = LoggerFactory.getLogger(Coordinator.class);

	private final MasterElector elector;

	@Inject
	public Coordinator(MasterElector elector) {
		this.elector = elector;
	}

	public MasterServer getElectedMaster() {
		String host = "localhost";
		int port = 8081;
		MasterServer server = new MasterServer(host, port, !elector.isMaster());
		return server;
	}

}
