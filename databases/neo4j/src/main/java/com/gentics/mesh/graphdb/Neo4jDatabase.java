package com.gentics.mesh.graphdb;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Graph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Neo4jDatabase extends AbstractDatabase {

	private static final Logger log = LoggerFactory.getLogger(Neo4jDatabase.class);

	private GraphDatabaseService graphDatabaseService;
	private Neo4j2Graph neo4jBlueprintGraph;

	@Override
	public void stop() {
		graphDatabaseService.shutdown();
		Database.setThreadLocalGraph(null);
	}

	@Override
	public FramedGraph startNonTransaction() {
		return new DelegatingFramedGraph<>(neo4jBlueprintGraph, true, false);
	}
	
	@Override
	public NoTrx noTrx() {
		return new Neo4jNoTrx(this);
	}

	@Override
	public FramedTransactionalGraph startTransaction() {
		return new DelegatingFramedTransactionalGraph<>(neo4jBlueprintGraph, true, false);
	}

	@Override
	public Trx trx() {
		return new Neo4jTrx(this);
	}
	
	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDatabaseService.shutdown();
			}
		});
	}

	@Override
	public void start() {
		String dbLocation = options.getDirectory();
		if (dbLocation == null) {
			graphDatabaseService = new TestGraphDatabaseFactory().newImpermanentDatabase();
		} else {
			File dbDir = new File(dbLocation);
			// TODO move this somewhere else or handle it by settings
			// GraphDatabaseBuilder builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbDir.getAbsolutePath());
			// graphDatabaseService = builder.newGraphDatabase();
			graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(dbDir.getAbsolutePath());
		}

		// Setup neo4j blueprint implementation
		neo4jBlueprintGraph = new Neo4j2Graph(graphDatabaseService);
		registerShutdownHook();

		// Add some indices
		// neo4jBlueprintGraph.createKeyIndex("name", Vertex.class);
		// neo4jBlueprintGraph.createKeyIndex("ferma_type", Vertex.class);
		// neo4jBlueprintGraph.createKeyIndex("ferma_type", Edge.class);

		// Neo4j2Graph graph = new Neo4j2Graph(graphDatabaseService());
		// //TODO configure indices
		// graph.createKeyIndex("ferma_type", Vertex.class);
		// graph.createKeyIndex("uuid", Vertex.class);
		// graph.createKeyIndex("ferma_type", Edge.class);
		// graph.createKeyIndex("uuid", Edge.class);
		// graph.createKeyIndex("languageTag", Edge.class);
		// graph.createKeyIndex("languageTag", Vertex.class);
		// graph.createKeyIndex("name", Vertex.class);
		// graph.createKeyIndex("key", Vertex.class);

	}

	public GraphDatabaseService getGraphDatabaseService() {
		return graphDatabaseService;
	}

	@Override
	public void reload(MeshElement element) {
		// Not needed
	}

	@Override
	public void importGraph(String importFile) throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public void exportGraph(String outputDirectory) throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public void backupGraph(String backupDirectory) throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public void restoreGraph(String backupFile) throws IOException {
		throw new NotImplementedException();
	}

}
