package com.gentics.mesh.neo4j;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.metric.MetricsService;
import com.syncleus.ferma.tx.Tx;
import com.tinkerpop.blueprints.Vertex;

@Ignore
public class Neo4jDatabaseTest {

	@Test
	public void testNeo4j() throws Exception {
		MeshOptions options = new MeshOptions();
		options.getStorageOptions().setDirectory("target/neo4j_" + System.currentTimeMillis());

		Neo4jTypeHandler typeHandler = new Neo4jTypeHandler();
		Neo4jIndexHandler indexHandler = new Neo4jIndexHandler();
		Neo4jClusterManager clusterManager = new Neo4jClusterManager();
		MetricsService metrics = Mockito.mock(MetricsService.class);
		Neo4jDatabase db = new Neo4jDatabase(metrics, typeHandler, indexHandler, clusterManager);

		db.init(options, "1.0", "com.gentics.mesh.neo4j");
		db.setupConnectionPool();

		try (Tx tx = db.tx()) {
			Vertex v = tx.getGraph().addVertex(null);
			tx.success();
		}

		db.closeConnectionPool();
		db.shutdown();

	}
}
