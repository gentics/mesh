package com.gentics.mesh.graphdb.tx.impl;

import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;

import com.arcadedb.database.Database;
import com.arcadedb.gremlin.ArcadeGraph;
import com.arcadedb.server.ArcadeDBServer;
import com.gentics.mesh.etc.config.GraphDBMeshOptions;
import com.gentics.mesh.graphdb.tx.AbstractArcadeStorage;
import com.gentics.mesh.graphdb.tx.ArcadeNoTx;
import com.gentics.mesh.metric.MetricsService;

/**
 * Storage implementation which utilizes the server context to access the database.
 */
public class ArcadeServerStorageImpl extends AbstractArcadeStorage {

	private final ArcadeDBServer context;
	private SharedArcadeGraphFactory factory;

	public ArcadeServerStorageImpl(GraphDBMeshOptions options, ArcadeDBServer context, MetricsService metrics) {
		super(options, metrics);
		this.context = context;
		this.factory = SharedArcadeGraphFactory.withContext(context, DB_NAME);
	}

	@Override
	public void open(String name) {
		context.getDatabase(name, true, true);
	}

	@Override
	public void close() {
		factory.close();
	}

	@Override
	public ArcadeGraph rawTx() {
		ArcadeGraph tx = factory.get();
		if (metrics.isEnabled()) {
			txCounter.increment();
		}
		return tx;
	}

	@Override
	public ArcadeNoTx rawNoTx() {
		Database noTx = factory.getLocalDatabase();
		if (metrics.isEnabled()) {
			noTxCounter.increment();
		}
		return new ArcadeNoTx(noTx);
	}

	@Override
	public void setMassInsertIntent() {
		// NOOP
	}

	@Override
	public void resetIntent() {
		// NOOP
	}

	@Override
	public void importGraph(String importFile) {
		throw new NotImplementedException("Not supported in server mode");
	}

	@Override
	public void exportGraph(String outputDirectory) {
		throw new NotImplementedException("Not supported in server mode");
	}

	@Override
	public void restore(String backupFile) throws IOException {
		throw new NotImplementedException("Not supported in server mode");
	}

	/**
	 * Create a new session by opening the mesh database.
	 */
	public Database createSession() {
		return context.getDatabase(DB_NAME, true, true);
	}

}
