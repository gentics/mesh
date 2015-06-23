package com.gentics.mesh.graphdb;

import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.ServerConfigurator;

import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
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
		 ServerConfigurator webConfig = new ServerConfigurator((GraphDatabaseAPI) graphDatabaseService);
		 WrappingNeoServerBootstrapper bootStrapper = new WrappingNeoServerBootstrapper((GraphDatabaseAPI) graphDatabaseService, webConfig);
		 bootStrapper.start();

		// Setup neo4j blueprint implementation
		Neo4j2Graph neo4jBlueprintGraph = new Neo4j2Graph(graphDatabaseService);
		registerShutdownHook(graphDatabaseService);

		// Add some indices
		neo4jBlueprintGraph.createKeyIndex("name", Vertex.class);
		neo4jBlueprintGraph.createKeyIndex("ferma_type", Vertex.class);
		neo4jBlueprintGraph.createKeyIndex("ferma_type", Edge.class);

		FramedTransactionalGraph fg = new DelegatingFramedTransactionalGraph<>(neo4jBlueprintGraph, true, false);
		return null;
		//return fg;
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
