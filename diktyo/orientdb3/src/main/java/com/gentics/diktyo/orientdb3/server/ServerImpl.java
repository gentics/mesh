package com.gentics.diktyo.orientdb3.server;

import javax.inject.Inject;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;

import com.gentics.diktyo.server.AbstractServer;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.server.OServer;

public class ServerImpl extends AbstractServer {

	@Inject
	public ServerImpl() {
	}

	@Override
	public void start() throws Exception {
		OServer server = OServer.startFromClasspathConfig("orientdb-server-config.xml");
		OrientDB context = server.getContext();
		try (OrientGraphFactory graph = new OrientGraphFactory(context, "test", ODatabaseType.MEMORY, "admin", "admin")) {

		}

		ODatabaseSession db = server.getContext().open("test", "admin", "admin");
		db.getMetadata().getSchema().existsClass("Test");
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}
