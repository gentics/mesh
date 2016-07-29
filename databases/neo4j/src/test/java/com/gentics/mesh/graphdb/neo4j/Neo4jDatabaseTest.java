package com.gentics.mesh.graphdb.neo4j;

import org.junit.Test;

import com.gentics.mesh.etc.GraphStorageOptions;
import com.gentics.mesh.graphdb.Neo4jDatabase;
import com.gentics.mesh.graphdb.Tx;

import io.vertx.core.Vertx;

public class Neo4jDatabaseTest extends AbstractNeo4jTest {

	@Test
	public void testDatabase() throws Exception {
		Neo4jDatabase db = new Neo4jDatabase();
		db.init(new GraphStorageOptions(), Vertx.vertx());

		try (Tx tx = db.tx()) {
			addPersonWithFriends(tx.getGraph(), "test");
		}
	}
}
