package com.gentics.mesh.graphdb.neo4j;

import org.junit.Test;

import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.Neo4jDatabase;
import com.gentics.mesh.graphdb.Trx;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public class Neo4jDatabaseTest extends AbstractDBTest {

	@Test
	public void testDatabase() {
		Neo4jDatabase db = new Neo4jDatabase();
		db.init(new StorageOptions());

		try (Trx tx = db.trx()) {
			addPersonWithFriends(tx.getGraph(), "test");
		}
	}
}
