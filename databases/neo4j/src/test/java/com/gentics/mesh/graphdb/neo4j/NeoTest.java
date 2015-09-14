package com.gentics.mesh.graphdb.neo4j;

import static java.lang.System.getProperty;

import java.io.File;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Graph;

public class NeoTest {

	public static void main(String[] args) {
		File dir = new File(getProperty("java.io.tmpdir"), "graph-db");
		GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(dir.getAbsolutePath());

		FramedTransactionalGraph fg = new DelegatingFramedTransactionalGraph<>(new Neo4j2Graph(db), true, false);

		try (Transaction tx = db.beginTx()) {
			db.createNode();
			Person p = addPersonWithFriends(fg, "test");
			System.out.println(p.getName());
			for(Person p2 : p.getFriends()) {
				System.out.println(p2.getName());
			}
			tx.success();
		} finally {
			db.shutdown();
		}
	}

	public static Person addPersonWithFriends(FramedGraph graph, String name) {
		Person p = graph.addFramedVertex(Person.class);
		p.setName(name);

		for (int i = 0; i < 10; i++) {
			Person friend = graph.addFramedVertex(Person.class);
			friend.setName("Friend " + i);
			p.addFriend(friend);
		}
		return p;
	}

}
