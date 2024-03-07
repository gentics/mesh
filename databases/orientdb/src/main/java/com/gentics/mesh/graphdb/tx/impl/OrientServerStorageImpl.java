package com.gentics.mesh.graphdb.tx.impl;

import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;

import com.gentics.mesh.etc.config.GraphDBMeshOptions;
import com.gentics.mesh.graphdb.tx.AbstractOrientStorage;
import com.gentics.mesh.metric.MetricsService;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;

/**
 * Storage implementation which utilizes the server context to access the database.
 */
public class OrientServerStorageImpl extends AbstractOrientStorage {

	private final OrientDB context;
	private final OrientGraphFactory factory;

	public OrientServerStorageImpl(GraphDBMeshOptions options, OrientDB context, MetricsService metrics) {
		super(options, metrics);
		this.context = context;
		this.factory = new OrientGraphFactory(context, DB_NAME, ODatabaseType.PLOCAL, OrientGraphFactory.ADMIN, OrientGraphFactory.ADMIN);
	}

	@Override
	public void open(String name) {
		context.createIfNotExists(DB_NAME, ODatabaseType.PLOCAL);
	}

	@Override
	public void close() {
		factory.close();
	}

	@Override
	public OrientGraph rawTx() {
		OrientGraph tx = factory.getTx();
		if (metrics.isEnabled()) {
			txCounter.increment();
		}
		return tx;
	}

	@Override
	public OrientGraph rawNoTx() {
		OrientGraph noTx = factory.getNoTx();
		if (metrics.isEnabled()) {
			noTxCounter.increment();
		}
		return noTx;
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
	public ODatabaseSession createSession() {
		return context.open(DB_NAME, "admin", "admin");
	}

}
