package com.gentics.mesh.graphdb.tx.impl;

import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.tx.AbstractOrientStorage;
import com.gentics.mesh.metric.MetricsService;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

/**
 * Storage implementation which utilizes the server context to access the database.
 */
public class OrientServerStorageImpl extends AbstractOrientStorage {

	private OrientDB context;

	public OrientServerStorageImpl(MeshOptions options, OrientDB context, MetricsService metrics) {
		super(options, metrics);
		this.context = context;
	}

	@Override
	public void open(String name) {
		// db = context.open(name, "admin", "admin");
		// if (db instanceof ODatabaseDocumentInternal) {
		// // internalNoTxFactory = OrientGraphFactory.getNoTxGraphImplFactory();
		// // ImplFactory internalTxFactory =
		// }
	}

	@Override
	public void close() {
		if (context.isOpen()) {
			context.close();
		}
		Orient.instance().shutdown();
	}

	@Override
	public OrientGraph rawTx() {
		txCouter.mark();
		ODatabaseSession db = createSession();
		return (OrientGraph) OrientGraphFactory.getTxGraphImplFactory().getGraph((ODatabaseDocumentInternal) db);
	}

	@Override
	public OrientGraphNoTx rawNoTx() {
		noTxCouter.mark();
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

	public ODatabaseSession createSession() {
		return context.open(DB_NAME, "admin", "admin");
	}

}
