package com.gentics.mesh.neo4j;

import static com.gentics.mesh.metric.SimpleMetric.TX;

import java.io.File;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.GraphStorage;
import com.gentics.mesh.metric.MetricsService;

import io.micrometer.core.instrument.Counter;

public class Neo4jStorage implements GraphStorage {

	private final MetricsService metrics;

	private final MeshOptions options;

	private final Counter txCounter;

	private GraphDatabaseService graphDb;

	public Neo4jStorage(MeshOptions options, MetricsService metrics) {
		this.options = options;
		this.metrics = metrics;
		this.txCounter = metrics.counter(TX);
	}

	public void open(String name) {
		GraphStorageOptions storageOptions = options.getStorageOptions();
		File storagePath = new File(storageOptions.getDirectory(), DB_NAME);
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(storagePath);
	}

	@Override
	public void close() {
		graphDb.shutdown();
		graphDb = null;
	}

	@Override
	public void clear() {
		try (Transaction tx = graphDb.beginTx();) {
			graphDb.execute("MATCH (n) DETACH DELETE n");
			tx.success();
		}
	}

	public GraphDatabaseService getGraphDb() {
		return graphDb;
	}

}
