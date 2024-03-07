package com.gentics.mesh.graphdb.tx.impl;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseFactory;
import com.arcadedb.gremlin.ArcadeGraph;
import com.gentics.mesh.etc.config.GraphDBMeshOptions;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.graphdb.tx.AbstractArcadeStorage;
import com.gentics.mesh.graphdb.tx.ArcadeStorage;
import com.gentics.mesh.metric.MetricsService;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Non-Clustered implementation of an {@link ArcadeStorage} which uses the {@link SharedArcadeGraphFactory} to provide transactions.
 */
public class OrientLocalStorageImpl extends AbstractArcadeStorage {

	private static final Logger log = LoggerFactory.getLogger(ArcadeLocalStorageImpl.class);

	private SharedArcadeGraphFactory factory;
	private String factoryLocal;

	public OrientLocalStorageImpl(GraphDBMeshOptions options, MetricsService metrics) {
		super(options, metrics);
	}

	@Override
	public void open(String name) {
		GraphStorageOptions storageOptions = options.getStorageOptions();
		if (storageOptions == null || storageOptions.getDirectory() == null) {
			log.info("No graph database settings found. Fallback to in memory mode.");
			factoryLocal = "memory:tinkerpop" + System.currentTimeMillis();
		} else {
			factoryLocal = "plocal:" + new File(storageOptions.getDirectory(), DB_NAME).getAbsolutePath();
		}
		factory = SharedArcadeGraphFactory.withLocal(factoryLocal);
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
	public Database rawNoTx() {
		Database notx = factory.getLocalDatabase();
		if (metrics.isEnabled()) {
			noTxCounter.increment();
		}
		return notx;
	}

	@Override
	public void setMassInsertIntent() {
		if (factory != null) {
			factory.getLocalDatabase().declareIntent(new OIntentMassiveInsert());
		}
	}

	@Override
	public void resetIntent() {
		if (factory != null) {
			factory.getLocalDatabase(false, false).declareIntent(null);
		}
	}

	@Override
	public void exportGraph(String outputDirectory) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("Running export to {" + outputDirectory + "} directory.");
		}
		Database db = factory.getLocalDatabase();
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
			ODatabaseExport export = new ODatabaseExport((ODatabaseDocumentInternal) db, new File(outputDirectory, exportFile).getAbsolutePath(), listener);
			export.exportDatabase();
			export.close();
		} finally {
			db.close();
		}

	}

	@Override
	public void importGraph(String importFile) throws IOException {
		ODatabaseDocument db = factory.getDatabase(false, false);
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};
			ODatabaseImport databaseImport = new ODatabaseImport((ODatabaseDocumentInternal) db, importFile, listener);
			databaseImport.importDatabase();
			databaseImport.close();
		} finally {
			db.close();
		}

	}

	@Override
	public ODatabaseSession createSession() {
		return (ODatabaseSession) factory.getDatabase(false, false);
	}
}
