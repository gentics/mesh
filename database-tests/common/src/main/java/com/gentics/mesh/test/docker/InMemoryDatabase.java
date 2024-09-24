package com.gentics.mesh.test.docker;

import java.io.File;
import java.util.Optional;

import org.hsqldb.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-Memory database instance, for testing purposes. Currently HSQLDB is used.
 * 
 * @author plyhun
 *
 */
public class InMemoryDatabase {

	protected static final Logger log = LoggerFactory.getLogger(InMemoryDatabase.class);
	
	private Optional<Server> maybeHsqlServer = Optional.empty();

	/**
	 * Start the server.
	 * 
	 * @param name DB name
	 * @param port port to start on
	 * @param diskStorageDirectory if filled, a normal disk storage is used.
	 * @param trace should the server logs be traced
	 */
	public void start(String name, int port, Optional<File> diskStorageDirectory, boolean trace) {
		try {
			maybeHsqlServer = Optional.of(new Server());
			Server hsqlServer = maybeHsqlServer.get();
			hsqlServer.setRestartOnShutdown(false);
			hsqlServer.setNoSystemExit(true);
			if (diskStorageDirectory.isEmpty()) {
				hsqlServer.setDatabasePath(0, "mem:tsg;hsqldb.tx=mvcc");
			} else {
				hsqlServer.setDatabasePath(0, "file:" + diskStorageDirectory.get().getAbsolutePath() + ";hsqldb.tx=mvcc");
			}						
			hsqlServer.setDatabaseName(0, name);
			hsqlServer.setPort(port);
			hsqlServer.setTrace(trace);
			hsqlServer.setSilent(!trace);
			hsqlServer.start();
			log.info("HSQLDB server started on port " + hsqlServer.getPort() + "...");
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Stop the server, if available.
	 */
	public void stop() {
		maybeHsqlServer.filter(s -> !s.isNotRunning()).ifPresent(Server::shutdown);
		maybeHsqlServer = Optional.empty();
	}

	public int getPort() {
		return maybeHsqlServer.map(Server::getPort).orElse(0);
	}
}
