package com.gentics.mesh.graphdb;

import java.io.File;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Graph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Neo4jDatabase extends AbstractDatabase {

	private static final Logger log = LoggerFactory.getLogger(Neo4jDatabase.class);

	private Neo4jThreadedTransactionalGraphWrapper wrapper;
	private GraphDatabaseService graphDatabaseService;

	@Override
	public void stop() {
		graphDatabaseService.shutdown();
		Trx.setLocalGraph(null);
	}

	@Override
	public void start() {
		String DB_LOCATION = options.getDirectory();
		File dbDir = new File(DB_LOCATION);

		// TODO move this somewhere else or handle it by settings
		// GraphDatabaseBuilder builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbDir.getAbsolutePath());
		// graphDatabaseService = builder.newGraphDatabase();
		graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(dbDir.getAbsolutePath());

		// Setup neo4j blueprint implementation
		Neo4j2Graph neo4jBlueprintGraph = new Neo4j2Graph(graphDatabaseService);
		registerShutdownHook(graphDatabaseService);

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
		// FramedTransactionalGraph framedGraph = new DelegatingFramedTransactionalGraph<Neo4j2Graph>(graph, true, false);

		wrapper = new Neo4jThreadedTransactionalGraphWrapper(neo4jBlueprintGraph);
		fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
	}

	public GraphDatabaseService getGraphDatabaseService() {
		return graphDatabaseService;
	}

	@Override
	public void reload(MeshElement element) {
		// Not supported
	}

}
