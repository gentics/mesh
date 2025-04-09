package com.gentics.mesh.test.docker;

import java.nio.charset.Charset;
import java.time.Duration;

import org.testcontainers.containers.GenericContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An extension to Docker {@link GenericContainer} for hosting the test databases.
 * 
 * @author plyhun
 *
 * @param <T>
 */
public abstract class DatabaseContainer<T extends DatabaseContainer<T>> extends GenericContainer<T> {

	public static final Charset UTF8 = Charset.forName("UTF-8");
	public static final String DEFAULT_USERNAME = "meshdbuser";
	public static final String DEFAULT_PASSWORD = "changeitNow4567&";
	public static final String DEFAULT_DATABASE = "mesh";

	protected static final Logger log = LoggerFactory.getLogger(DatabaseContainer.class);

	public DatabaseContainer(String container) {
		super(container);
	}

	@Override
	protected void configure() {
		withExposedPorts(getDatabasePort());
		withStartupTimeout(Duration.ofMinutes(5));
	}

	/**
	 * Get the port the test context should use for the database connectivity.
	 * 
	 * @return
	 */
	public int getMappedPort() {
		int dbPort = getDatabasePort();
		int mPort = getMappedPort(dbPort);
		//System.err.println("DB PORT MAPPED: " + dbPort + ":" + mPort);
		return mPort;
	}

	/**
	 * Get the port of an original hosted database.
	 * 
	 * @return
	 */
	public abstract int getDatabasePort();
}
