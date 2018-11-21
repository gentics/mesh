package com.gentics.diktyo.orientdb3.server;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.diktyo.server.Server;
import com.gentics.diktyo.server.ServerManager;

@Singleton
public class ServerManagerImpl implements ServerManager {

	private final Server server;

	@Inject
	public ServerManagerImpl(Server server) {
		this.server = server;
	}

	@Override
	public Server server() {
		return server;
	}

}
