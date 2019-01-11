package com.gentics.mesh.graphdb.tx.impl;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.tx.AbstractOrientStorage;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Storage implementation which utilizes the server context to access the database.
 */
public class OrientServerStorageImpl extends AbstractOrientStorage {

	private static final Logger log = LoggerFactory.getLogger(OrientServerStorageImpl.class);

	private OrientDB context;

	public OrientServerStorageImpl(MeshOptions options, OrientDB context) {
		super(options);
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
		ODatabaseSession db = context.open(DB_NAME, "admin", "admin");
		return (OrientGraph) OrientGraphFactory.getTxGraphImplFactory().getGraph((ODatabaseDocumentInternal) db);
	}

	@Override
	public OrientGraphNoTx rawNoTx() {
		ODatabaseSession db = context.open(DB_NAME, "admin", "admin");
		return (OrientGraphNoTx) OrientGraphFactory.getNoTxGraphImplFactory().getGraph((ODatabaseDocumentInternal) db);
	}

	@Override
	public void setMassInsertIntent() {
	}

	@Override
	public void resetIntent() {
	}

	@Override
	public void backup(String backupDirectory) {
	}

	@Override
	public void restore(String backupFile) {
	}

	@Override
	public void importGraph(String importFile) {
	}

	@Override
	public void exportGraph(String outputDirectory) {
	}

}
