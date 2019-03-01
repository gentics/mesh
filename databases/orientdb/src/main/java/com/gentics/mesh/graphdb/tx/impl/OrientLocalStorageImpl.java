package com.gentics.mesh.graphdb.tx.impl;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.tx.AbstractOrientStorage;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OrientLocalStorageImpl extends AbstractOrientStorage {

	private static final Logger log = LoggerFactory.getLogger(OrientLocalStorageImpl.class);

	private OrientGraphFactory factory;

	public OrientLocalStorageImpl(MeshOptions options) {
		super(options);
	}

	@Override
	public void open(String name) {
		GraphStorageOptions storageOptions = options.getStorageOptions();
		if (storageOptions == null || storageOptions.getDirectory() == null) {
			log.info("No graph database settings found. Fallback to in memory mode.");
			factory = new OrientGraphFactory("memory:tinkerpop").setupPool(16, 100);
		} else {
			factory = new OrientGraphFactory("plocal:" + new File(storageOptions.getDirectory(), DB_NAME).getAbsolutePath()).setupPool(16, 100);
		}
	}

	@Override
	public void close() {
		factory.close();
	}

	@Override
	public OrientGraph rawTx() {
		return factory.getTx();
	}

	@Override
	public OrientGraphNoTx rawNoTx() {
		return factory.getNoTx();
	}

	@Override
	public void setMassInsertIntent() {
		if (factory != null) {
			factory.declareIntent(new OIntentMassiveInsert());
		}
	}

	@Override
	public void resetIntent() {
		if (factory != null) {
			factory.declareIntent(null);
		}
	}

	@Override
	public void exportGraph(String outputDirectory) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("Running export to {" + outputDirectory + "} directory.");
		}
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};

			String dateString = formatter.format(new Date());
			String exportFile = "export_" + dateString;
			new File(outputDirectory).mkdirs();
			ODatabaseExport export = new ODatabaseExport(db, new File(outputDirectory, exportFile).getAbsolutePath(), listener);
			export.exportDatabase();
			export.close();
		} finally {
			db.close();
		}

	}

	@Override
	public void importGraph(String importFile) throws IOException {
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};
			ODatabaseImport databaseImport = new ODatabaseImport(db, importFile, listener);
			databaseImport.importDatabase();
			databaseImport.close();
		} finally {
			db.close();
		}

	}

	@Override
	public ODatabaseSession createSession() {
		return factory.getDatabase();
	}
}
