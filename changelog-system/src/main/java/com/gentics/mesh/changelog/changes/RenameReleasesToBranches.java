package com.gentics.mesh.changelog.changes;

import static com.tinkerpop.blueprints.Direction.IN;
import static com.tinkerpop.blueprints.Direction.OUT;

import java.util.PriorityQueue;
import java.util.Queue;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

public class RenameReleasesToBranches extends AbstractChange {

	private static long EDGE_BATCH_LIMIT = 200_000;

	private static long VERTEX_BATCH_LIMIT = 10_000;

	@Override
	public String getName() {
		return "ReleaseBranchRenameChange";
	}

	@Override
	public String getDescription() {
		return "Renames the release vertices, edges and properties to branch";
	}

	@Override
	public void applyOutsideTx() {
		getDb().type().addVertexType("BranchImpl", "MeshVertexImpl");
		getDb().type().addVertexType("BranchRootImpl", "MeshVertexImpl");
		getDb().type().addVertexType("BranchMigrationJobImpl", "MeshVertexImpl");

		runBatchAction(() -> migrateVertices("ReleaseImpl", "BranchImpl", VERTEX_BATCH_LIMIT));
		runBatchAction(() -> migrateVertices("ReleaseRootImpl", "BranchRootImpl", VERTEX_BATCH_LIMIT));
		runBatchAction(() -> migrateVertices("ReleaseMigrationJobImpl", "BranchMigrationJobImpl", VERTEX_BATCH_LIMIT));

		migrateEdgeProps("HAS_TAG", EDGE_BATCH_LIMIT);
		migrateEdgeProps("HAS_FIELD_CONTAINER", EDGE_BATCH_LIMIT);
		migrateEdgeProps("HAS_PARENT_NODE", EDGE_BATCH_LIMIT);

	}

	private void migrateEdgeProps(String label, long limit) {
		PriorityQueue<Object> jobQueue = new PriorityQueue<>();
		TransactionalGraph graph = getDb().rawTx();
		setGraph(graph);
		try {
			int r = 0;
			for (Edge edge : getGraph().getEdges("@class", label)) {
				jobQueue.add(edge.getId());
				r++;
				if (r % 10_000 == 0) {
					log.info("Added element to job queue " + jobQueue.size());
				}
			}
		} finally {
			graph.shutdown();
		}
		log.info("Created queue for " + label + " (" + jobQueue.size() + " entries)");
		runBatchAction(() -> migrateBranchEdgeProperties(label, jobQueue, limit));

	}

	@Override
	public void applyInTx() {
		Vertex meshRoot = getMeshRootVertex();

		Vertex projectRoot = meshRoot.getVertices(OUT, "HAS_PROJECT_ROOT").iterator().next();
		for (Vertex project : projectRoot.getVertices(OUT, "HAS_PROJECT")) {

			Iterable<Edge> it = project.getEdges(OUT, "HAS_RELEASE_ROOT");
			for (Edge edge : it) {
				migrateEdge(edge, "HAS_BRANCH_ROOT", true);

				// Iterate over all releases
				Vertex in = edge.getVertex(IN);
				for (Edge edge1 : in.getEdges(OUT, "HAS_RELEASE")) {
					Vertex release = edge1.getVertex(IN);
					for (Edge nextReleaseEdge : release.getEdges(OUT, "HAS_NEXT_RELEASE")) {
						migrateEdge(nextReleaseEdge, "HAS_NEXT_BRANCH", true);
					}
					migrateEdge(edge1, "HAS_BRANCH", true);
				}

				for (Edge edge1 : in.getEdges(OUT, "HAS_INITIAL_RELEASE")) {
					migrateEdge(edge1, "HAS_INITIAL_BRANCH", true);
				}

				for (Edge edge1 : in.getEdges(OUT, "HAS_LATEST_RELEASE")) {
					migrateEdge(edge1, "HAS_LATEST_BRANCH", true);
				}
			}

		}

		migrateJobProperties();
	}

	private void migrateJobProperties() {
		Vertex meshRoot = getMeshRootVertex();
		for (Vertex root : meshRoot.getVertices(OUT, "HAS_JOB_ROOT")) {
			for (Vertex job : root.getVertices(OUT, "HAS_JOB")) {
				migrateProperty(job, "releaseName", "branchName");
				migrateProperty(job, "releaseUuid", "branchUuid");
			}
		}
	}

	private boolean migrateBranchEdgeProperties(String label, Queue<Object> jobQueue, long limit) {
		long n = 0;
		while (!jobQueue.isEmpty()) {
			Object id = jobQueue.poll();
			Edge edge = getGraph().getEdge(id);
			migrateProperty(edge, "releaseUuid", "branchUuid");
			n++;
			double percent = 1;
			if (jobQueue.size() != 0) {
				percent = (double) n / (double) jobQueue.size();
			}
			if (n % 8_000 == 0) {
				log.info("Migrated {" + n + "} " + label + " edges. Remaining " + jobQueue.size() + " (" + (percent * 100) + "%)");
				getGraph().commit();
			}
			if (n > limit) {
				log.info("Limit for batch reached. Remaining " + jobQueue.size());
				return true;
			}
		}
		log.info("Migrated {" + n + "} " + label + " edges. Remaining " + jobQueue.size());
		return !jobQueue.isEmpty();
	}

	private boolean migrateVertices(String from, String to, long limit) {
		log.info("Migrating vertex type {" + from + "} to {" + to + "}");
		Iterable<Vertex> it = getGraph().getVertices("@class", from);
		long count = 0;
		for (Vertex fromV : it) {
			// Create new vertex with new type
			Vertex toV = getGraph().addVertex("class:" + to);
			// Duplicate the in edges
			for (Edge inE : fromV.getEdges(Direction.IN)) {
				Vertex out = inE.getVertex(OUT);
				Edge e = out.addEdge(inE.getLabel(), toV);
				for (String key : inE.getPropertyKeys()) {
					e.setProperty(key, inE.getProperty(key));
				}
			}
			// Duplicate the out edges
			for (Edge outE : fromV.getEdges(Direction.OUT)) {
				Vertex in = outE.getVertex(IN);
				Edge e = toV.addEdge(outE.getLabel(), in);
				for (String key : outE.getPropertyKeys()) {
					e.setProperty(key, outE.getProperty(key));
				}
			}
			// Duplicate properties
			for (String key : fromV.getPropertyKeys()) {
				toV.setProperty(key, fromV.getProperty(key));
			}

			// Update the ferma type
			toV.setProperty("ferma_type", to);
			fromV.remove();
			count++;
			if (count % 1000 == 0) {
				log.info("Migrated {" + count + "} vertices.");
				getGraph().commit();
			}
			if (count >= limit) {
				log.info("Limit for batch reached");
				return true;
			}
		}
		log.info("Migrated total of {" + count + "} vertices from {" + from + "} to {" + to + "}");
		if (count == 0) {
			return false;
		}
		return true;
	}

	private void migrateProperty(Vertex vertex, String oldPropertyKey, String newPropertyKey) {
		log.info("Migrating vertex: " + vertex.getId());
		String value = vertex.getProperty(oldPropertyKey);
		vertex.removeProperty(oldPropertyKey);
		if (value != null) {
			vertex.setProperty(newPropertyKey, value);
		}
	}

	private void migrateProperty(Edge edge, String oldPropertyKey, String newPropertyKey) {
		String value = edge.getProperty(oldPropertyKey);
		edge.removeProperty(oldPropertyKey);
		if (value != null) {
			edge.setProperty(newPropertyKey, value);
		}
	}

	// private Vertex migrateType(Vertex vertex, String newType) {
	// String type = vertex.getProperty("ferma_type");
	// vertex.setProperty("ferma_type", newType);
	// Vertex newVertex = getDb().changeType(vertex, newType, getGraph());
	// log.info("Migrating {" + type + "} to {" + newType + "}");
	// return newVertex;
	// }

	private void migrateEdge(Edge edge, String newLabel, boolean reverseOrder) {
		Vertex in = edge.getVertex(IN);
		Vertex out = edge.getVertex(OUT);
		if (reverseOrder) {
			out.addEdge(newLabel, in);
		} else {
			in.addEdge(newLabel, out);
		}
		edge.remove();
	}

	@Override
	public String getUuid() {
		return "53C59D7EBC94483B859D7EBC94083BD6";
	}

}
