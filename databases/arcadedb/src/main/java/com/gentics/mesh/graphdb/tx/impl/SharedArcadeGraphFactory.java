package com.gentics.mesh.graphdb.tx.impl;

import java.io.Closeable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration2.Configuration;

import com.arcadedb.database.BasicDatabase;
import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseFactory;
import com.arcadedb.gremlin.ArcadeGraph;
import com.arcadedb.remote.RemoteDatabase;
import com.arcadedb.server.ArcadeDBServer;

/**
 * ArcadeDB Gremlin implementation factory class. Utilizes a pool of ArcadeGraph
 * to avoid creating a new instance every time. A copy of
 * {@link com.arcadedb.gremlin.ArcadeGraphFactory}, extended to expose a single
 * shared Database instance.
 *
 * @author Luca Garulli (l.garulli@arcadedata.com)
 */
public class SharedArcadeGraphFactory implements Closeable {
	private final ConcurrentLinkedQueue<PooledArcadeGraph> pooledInstances = new ConcurrentLinkedQueue<>();
	private final Database localDatabase;
	private final String host;
	private final int port;
	private final String databaseName;
	private final String userName;
	private final String userPassword;
	private int maxInstances = 4096;
	private final AtomicInteger totalInstancesCreated = new AtomicInteger(0);

	public class PooledArcadeGraph extends ArcadeGraph {
		private final SharedArcadeGraphFactory factory;

		protected PooledArcadeGraph(final SharedArcadeGraphFactory factory, final Configuration configuration) {
			super(configuration);
			this.factory = factory;
		}

		protected PooledArcadeGraph(final SharedArcadeGraphFactory factory, final BasicDatabase database) {
			super(database);
			this.factory = factory;
		}

		@Override
		public void close() {
			factory.release(this);
		}

		public void dispose() {
			super.close();
		}
	}

	/**
	 * Creates a new ArcadeGraphFactory with remote database connection. By default
	 * maximum 32 instances of ArcadeGraph can be created. You can change this
	 * configuration with the method #setMaxInstances().
	 *
	 * @param host         ArcadeDB remote server ip address or host name
	 * @param port         ArcadeDB remote server TCP/IP port
	 * @param databaseName Database name
	 * @param userName     User name
	 * @param userPassword User password
	 */
	private SharedArcadeGraphFactory(final String host, final int port, final String databaseName,
			final String userName, final String userPassword) {
		this.host = host;
		this.port = port;
		this.databaseName = databaseName;
		this.userName = userName;
		this.userPassword = userPassword;
		this.localDatabase = null;
	}

	/**
	 * Creates a new ArcadeGraphFactory with local database connection. By default
	 * maximum 32 instances of ArcadeGraph can be created. You can change this
	 * configuration with the method #setMaxInstances().
	 *
	 * @param databasePath ArcadeDB local database path
	 */
	@SuppressWarnings("resource")
	private SharedArcadeGraphFactory(final String databasePath) {
		this.localDatabase = new DatabaseFactory(databasePath).open();
		this.host = null;
		this.port = 0;
		this.databaseName = null;
		this.userName = null;
		this.userPassword = null;
	}

	private SharedArcadeGraphFactory(final ArcadeDBServer context, String databaseName) {
		this.localDatabase = context.getOrCreateDatabase(databaseName);
		this.host = null;
		this.port = 0;
		this.databaseName = null;
		this.userName = null;
		this.userPassword = null;
	}

	public static SharedArcadeGraphFactory withRemote(final String host, final int port, final String databaseName,
			final String userName, final String userPassword) {
		return new SharedArcadeGraphFactory(host, port, databaseName, userName, userPassword);
	}

	public static SharedArcadeGraphFactory withLocal(final String databasePath) {
		return new SharedArcadeGraphFactory(databasePath);
	}

	public static SharedArcadeGraphFactory withContext(final ArcadeDBServer context, String databaseName) {
		return new SharedArcadeGraphFactory(context, databaseName);
	}

	/**
	 * Closes the factory and dispose all the remaining ArcadeGraph instances in the
	 * pool.
	 */
	@Override
	public void close() {
		while (!pooledInstances.isEmpty()) {
			final PooledArcadeGraph instance = pooledInstances.poll();
			if (instance != null)
				instance.dispose();
		}

		if (localDatabase != null)
			localDatabase.close();
	}

	public ArcadeGraph get() {
		PooledArcadeGraph instance = pooledInstances.poll();
		if (instance == null) {
			if (totalInstancesCreated.get() >= maxInstances)
				throw new IllegalArgumentException("Unable to create more than " + maxInstances
						+ " instances in the pool. Assure the instances were correctly released with Graph.close()");

			if (localDatabase != null)
				instance = new PooledArcadeGraph(this, localDatabase);
			else
				instance = new PooledArcadeGraph(this,
						new RemoteDatabase(host, port, databaseName, userName, userPassword));
			totalInstancesCreated.incrementAndGet();
		}
		return instance;
	}

	public void setMaxInstances(final int maxInstances) {
		this.maxInstances = maxInstances;
	}

	public int getMaxInstances() {
		return maxInstances;
	}

	public int getTotalInstancesCreated() {
		return totalInstancesCreated.get();
	}

	private void release(final PooledArcadeGraph pooledArcadeGraph) {
		pooledInstances.offer(pooledArcadeGraph);
	}

	public Database getLocalDatabase() {
		return localDatabase;
	}
}
