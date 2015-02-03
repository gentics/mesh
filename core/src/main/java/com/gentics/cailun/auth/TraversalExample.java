package com.gentics.cailun.auth;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.graphdb.traversal.UniquenessFactory;

public class TraversalExample {
	private GraphDatabaseService db;
	// private TraversalDescription friendsTraversal;

	private static final String DB_PATH = "/tmp/graphdb";

	public static void main(String[] args) {
		GraphDatabaseService database = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
		TraversalExample example = new TraversalExample(database);
		example.run();
	}

	public TraversalExample(GraphDatabaseService db) {
		this.db = db;
		// friendsTraversal = db.traversalDescription().depthFirst().relationships(Rels.HAS_ROLE).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL);
	}

	private void run() {
		try (Transaction tx = db.beginTx()) {
			Node user = db.getNodeById(0L);
			Node page = db.getNodeById(22L);
			System.out.println("User: " + user.getProperty("username") + " | " + user.getLabels());

			long now = System.currentTimeMillis();
			for (int i = 0; i < 100000; i++) {
				knowsLikesTraverser(user, page);
			}
			long duration = System.currentTimeMillis() - now;
			System.out.println(duration);
			// System.out.println(traverseBaseTraverser(joe));
			// System.out.println(depth3(joe));
			// System.out.println(depth4(joe));
			// System.out.println(nodes(joe));
			// System.out.println(relationships(joe));
		}
	}

	public boolean knowsLikesTraverser(Node node, Node targetPage) {
		for (Relationship rel : db.traversalDescription().depthFirst().relationships(Rels.MEMBER_OF, Direction.OUTGOING)
				.relationships(Rels.HAS_ROLE, Direction.INCOMING).relationships(Rels.HAS_PERMISSIONSET, Direction.OUTGOING)
				.uniqueness(Uniqueness.RELATIONSHIP_GLOBAL).traverse(node).relationships()) {

			if ("HAS_PERMISSIONSET".equalsIgnoreCase(rel.getType().name())) {
				if ((boolean) rel.getProperty("canRead") == true) {
					if (rel.getEndNode().getId() == targetPage.getId()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// public String traverseBaseTraverser(Node node) {
	// String output = "";
	// for (Path path : friendsTraversal.traverse(node)) {
	// output += path + "\n";
	// }
	// return output;
	// }
	//
	// public String depth3(Node node) {
	// String output = "";
	// for (Path path : friendsTraversal.evaluator(Evaluators.toDepth(3)).traverse(node)) {
	// output += path + "\n";
	// }
	// return output;
	// }
	//
	// public String depth4(Node node) {
	// String output = "";
	// for (Path path : friendsTraversal.evaluator(Evaluators.fromDepth(2)).evaluator(Evaluators.toDepth(4)).traverse(node)) {
	// output += path + "\n";
	// }
	// return output;
	// }
	//
	// public String nodes(Node node) {
	// String output = "";
	// for (Node currentNode : friendsTraversal.traverse(node).nodes()) {
	// output += currentNode.getId() + " | " + currentNode.getLabels().toString() + "\n";
	// }
	// return output;
	// }
	//
	// public String relationships(Node node) {
	// String output = "";
	// for (Relationship relationship : friendsTraversal.traverse(node).relationships()) {
	// output += relationship.getType().name() + "\n";
	// }
	// return output;
	// }

	private enum Rels implements RelationshipType {
		HAS_PERMISSIONSET, MEMBER_OF, HAS_ROLE
	}
}
