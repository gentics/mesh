package com.gentics.mesh.graphdb;

import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Graph;

public class Neo4jDatabaseProviderImpl implements DatabaseServiceProvider {

	@Override
	public FramedThreadedTransactionalGraph getFramedGraph(JsonObject settings) throws IOException {

		String DB_LOCATION = "/tmp/graphdb";
		File dbDir = new File(DB_LOCATION);
		//TODO move this somewhere else or handle it by settings
		FileUtils.deleteDirectory(dbDir);
		GraphDatabaseBuilder builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbDir.getAbsolutePath());
		GraphDatabaseService graphDatabaseService = builder.newGraphDatabase();
		// Start the neo4j web console - by default it can be accessed using http://localhost:7474. It is handy for development and should not be enabled by
		// default.
		//		ServerConfigurator webConfig = new ServerConfigurator((GraphDatabaseAPI) graphDatabaseService);
		//		WrappingNeoServerBootstrapper bootStrapper = new WrappingNeoServerBootstrapper((GraphDatabaseAPI) graphDatabaseService, webConfig);
		//		bootStrapper.start();

		// Setup neo4j blueprint implementation
		Neo4j2Graph neo4jBlueprintGraph = new Neo4j2Graph(graphDatabaseService);
		registerShutdownHook(graphDatabaseService);

		// Add some indices
		neo4jBlueprintGraph.createKeyIndex("name", Vertex.class);
		neo4jBlueprintGraph.createKeyIndex("ferma_type", Vertex.class);
		neo4jBlueprintGraph.createKeyIndex("ferma_type", Edge.class);

		//		Neo4j2Graph graph = new Neo4j2Graph(graphDatabaseService());
		//		//TODO configure indices
		//		graph.createKeyIndex("ferma_type", Vertex.class);
		//		graph.createKeyIndex("uuid", Vertex.class);
		//		graph.createKeyIndex("ferma_type", Edge.class);
		//		graph.createKeyIndex("uuid", Edge.class);
		//		graph.createKeyIndex("languageTag", Edge.class);
		//		graph.createKeyIndex("languageTag", Vertex.class);
		//		graph.createKeyIndex("name", Vertex.class);
		//		graph.createKeyIndex("key", Vertex.class);
		//		FramedTransactionalGraph framedGraph = new DelegatingFramedTransactionalGraph<Neo4j2Graph>(graph, true, false);

		ThreadedTransactionalGraphWrapper wrapper = new Neo4jThreadedTransactionalGraphWrapper(neo4jBlueprintGraph);

		FramedThreadedTransactionalGraph fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
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
