package com.gentics.mesh.graphdb.tx.impl;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseFactory;
import com.arcadedb.gremlin.ArcadeGraph;
import com.arcadedb.integration.exporter.Exporter;
import com.arcadedb.integration.importer.Importer;
import com.gentics.mesh.etc.config.GraphDBMeshOptions;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.graphdb.tx.AbstractArcadeStorage;
import com.gentics.mesh.graphdb.tx.ArcadeNoTx;
import com.gentics.mesh.graphdb.tx.ArcadeStorage;
import com.gentics.mesh.metric.MetricsService;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Non-Clustered implementation of an {@link ArcadeStorage} which uses the {@link SharedArcadeGraphFactory} to provide transactions.
 */
public class ArcadeLocalStorageImpl extends AbstractArcadeStorage {

	private static final Logger log = LoggerFactory.getLogger(ArcadeLocalStorageImpl.class);

	private SharedArcadeGraphFactory factory;
	private String factoryLocal;

	public ArcadeLocalStorageImpl(GraphDBMeshOptions options, MetricsService metrics) {
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
	public ArcadeNoTx rawNoTx() {
		Database notx = factory.getLocalDatabase();
		if (metrics.isEnabled()) {
			noTxCounter.increment();
		}
		return new ArcadeNoTx(notx);
	}

	@Override
	public void setMassInsertIntent() {
	}

	@Override
	public void resetIntent() {
	}

	@Override
	public void exportGraph(String outputDirectory) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("Running export to {" + outputDirectory + "} directory.");
		}
		try (Database db = createSession()) {
			String dateString = formatter.format(new Date());
			String exportFile = "export_" + dateString;
			new File(outputDirectory).mkdirs();
			String absolutePath = new File(outputDirectory, exportFile).getAbsolutePath();
			Exporter exporter = new Exporter(db, absolutePath);
			exporter.setOverwrite(false);
			Map<String, Object> results = exporter.exportDatabase();
			log.info("Database exported.\n{}", results.entrySet().stream().map(entry -> String.format("\t$s: $s", entry.getKey(), Objects.toString(entry.getValue()))).collect(Collectors.joining("\n")));
		}
	}

	@Override
	public void importGraph(String importFile) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("Running import from {" + importFile + "}.");
		}
		try (Database db = createSession()) {
			Importer importer = new Importer(db, importFile);
			Map<String, Object> results = importer.load();
			log.info("Database imported.\n{}", results.entrySet().stream().map(entry -> String.format("\t$s: $s", entry.getKey(), Objects.toString(entry.getValue()))).collect(Collectors.joining("\n")));
		}
	}

	@Override
	public Database createSession() {
		return new DatabaseFactory(factoryLocal).open();
	}
}
