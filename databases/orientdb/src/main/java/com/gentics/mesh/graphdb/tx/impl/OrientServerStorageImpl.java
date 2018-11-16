package com.gentics.mesh.graphdb.tx.impl;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.tx.AbstractOrientStorage;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OrientServerStorageImpl extends AbstractOrientStorage {

	private static final Logger log = LoggerFactory.getLogger(OrientServerStorageImpl.class);

	private OrientDB context;

	private ODatabaseSession db;

	public OrientServerStorageImpl(MeshOptions options, OrientDB context) {
		super(options);
		this.context = context;
	}

	@Override
	public void open(String name) {
		db = context.open(name, "admin", "admin");
		// if (db instanceof ODatabaseDocumentInternal) {
		// // internalNoTxFactory = OrientGraphFactory.getNoTxGraphImplFactory();
		// // ImplFactory internalTxFactory =
		// }
	}

	@Override
	public void close() {
		db.close();
		Orient.instance().shutdown();
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
	}

	@Override
	public TransactionalGraph rawTx() {
		return (OrientGraph) OrientGraphFactory.getTxGraphImplFactory().getGraph((ODatabaseDocumentInternal) db);
	}

	@Override
	public OrientGraphNoTx rawNoTx() {
		return (OrientGraphNoTx) OrientGraphFactory.getNoTxGraphImplFactory().getGraph((ODatabaseDocumentInternal) db);
	}

	@Override
	public void setMassInsertIntent() {
		// TODO Auto-generated method stub
	}

	@Override
	public void resetIntent() {
		// TODO Auto-generated method stub
	}

	@Override
	public void backup(String backupDirectory) {
		// TODO Auto-generated method stub

	}

	@Override
	public void restore(String backupFile) {
		// TODO Auto-generated method stub

	}

	@Override
	public void importGraph(String importFile) {
		// TODO Auto-generated method stub

	}

	@Override
	public void exportGraph(String outputDirectory) {
		// TODO Auto-generated method stub

	}

}
