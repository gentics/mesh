package com.gentics.mesh.graphdb.tx.impl;

import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.graphdb.tx.AbstractOrientStorage;
import com.gentics.mesh.metric.MetricsService;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

/**
 * Storage implementation which utilizes the server context to access the database.
 */
public class OrientServerStorageImpl extends AbstractOrientStorage {

	private OrientDB context;

	public OrientServerStorageImpl(OrientDBMeshOptions options, OrientDB context, MetricsService metrics) {
		super(options, metrics);
		this.context = context;
	}

	@Override
	public void open(String name) {
		context.createIfNotExists(DB_NAME, ODatabaseType.PLOCAL);
	}

	@Override
	public void close() {
	}

	@Override
	public OrientGraph rawTx() {
		if (metrics.isEnabled()) {
			txCounter.increment();
		}
		ODatabaseSession db = createSession();
		return (OrientGraph) OrientGraphFactory.getTxGraphImplFactory().getGraph((ODatabaseDocumentInternal) db);
	}

	@Override
	public OrientGraphNoTx rawNoTx() {
		if (metrics.isEnabled()) {
			noTxCounter.increment();
		}
		ODatabaseSession db = createSession();
		return (OrientGraphNoTx) OrientGraphFactory.getNoTxGraphImplFactory().getGraph((ODatabaseDocumentInternal) db);
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
