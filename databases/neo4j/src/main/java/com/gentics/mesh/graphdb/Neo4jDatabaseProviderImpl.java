package com.gentics.mesh.graphdb;

import io.vertx.core.json.JsonObject;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Graph;

public class Neo4jDatabaseProviderImpl implements DatabaseServiceProvider {

	@Override
	public FramedTransactionalGraph getFramedGraph(JsonObject settings) {

		String DB_LOCATION = "/tmp/graphdb";
		GraphDatabaseBuilder builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(DB_LOCATION);
		GraphDatabaseService graphDatabaseService = builder.newGraphDatabase();
		// Start the neo4j web console - by default it can be accessed using http://localhost:7474. It is handy for development and should not be enabled by
		// default.
		// ServerConfigurator webConfig = new ServerConfigurator((GraphDatabaseAPI) graphDatabaseService);
		// WrappingNeoServerBootstrapper bootStrapper = new WrappingNeoServerBootstrapper((GraphDatabaseAPI) graphDatabaseService, webConfig);
		// bootStrapper.start();

		// Setup neo4j blueprint implementation
		Neo4j2Graph neo4jBlueprintGraph = new Neo4j2Graph(graphDatabaseService);
		registerShutdownHook(graphDatabaseService);

		// Add some indices
		neo4jBlueprintGraph.createKeyIndex("name", Vertex.class);
		neo4jBlueprintGraph.createKeyIndex("ferma_type", Vertex.class);
		neo4jBlueprintGraph.createKeyIndex("ferma_type", Edge.class);

		FramedTransactionalGraph fg = new DelegatingFramedTransactionalGraph<>(neo4jBlueprintGraph, true, false);
		return fg;
	}

	private void registerShutdownHook(final GraphDatabaseService graphDatabaseService) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDatabaseService.shutdown();
			}
		});
	}

}
