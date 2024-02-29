package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.util.StreamUtil;

/**
 * Changelog entry which renames releases to branches.
 */
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

		try {
			migrateEdgeProps("HAS_TAG", EDGE_BATCH_LIMIT);
			migrateEdgeProps("HAS_FIELD_CONTAINER", EDGE_BATCH_LIMIT);
			migrateEdgeProps("HAS_PARENT_NODE", EDGE_BATCH_LIMIT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void migrateEdgeProps(String label, long limit) throws Exception {
		PriorityQueue<Object> jobQueue = new PriorityQueue<>();
		Graph graph = getDb().rawTx();
		setGraph(graph);
		try (DefaultGraphTraversal<?, Vertex> t = new DefaultGraphTraversal<>(getGraph())) {
			int r = 0;
			Iterable<Edge> edges = StreamUtil.toIterable(t.E().has(ElementFrame.TYPE_RESOLUTION_KEY, label));
			for (Edge edge : edges) {
				jobQueue.add(edge.id());
				r++;
				if (r % 10_000 == 0) {
					log.info("Added element to job queue " + jobQueue.size());
				}
			}
		} finally {
			graph.close();
		}
		log.info("Created queue for " + label + " (" + jobQueue.size() + " entries)");
		runBatchAction(() -> migrateBranchEdgeProperties(label, jobQueue, limit));
	}

	@Override
	public void applyInTx() {
		Vertex meshRoot = getMeshRootVertex();
		Iterator<Vertex> iter = meshRoot.vertices(OUT, "HAS_PROJECT_ROOT");
		if (!iter.hasNext()) {
			log.info("RenameReleasesToBranches change skipped");
			return;
		}
		Vertex projectRoot = iter.next();
		for (Vertex project : StreamUtil.toIterable(projectRoot.vertices(OUT, "HAS_PROJECT"))) {

			Iterable<Edge> it = StreamUtil.toIterable(project.edges(OUT, "HAS_RELEASE_ROOT"));
			for (Edge edge : it) {
				migrateEdge(edge, "HAS_BRANCH_ROOT", true);

				// Iterate over all releases
				Vertex in = edge.inVertex();
				for (Edge edge1 : StreamUtil.toIterable(in.edges(OUT, "HAS_RELEASE"))) {
					Vertex release = edge1.inVertex();
					for (Edge nextReleaseEdge : StreamUtil.toIterable(release.edges(OUT, "HAS_NEXT_RELEASE"))) {
						migrateEdge(nextReleaseEdge, "HAS_NEXT_BRANCH", true);
					}
					migrateEdge(edge1, "HAS_BRANCH", true);
				}

				for (Edge edge1 : StreamUtil.toIterable(in.edges(OUT, "HAS_INITIAL_RELEASE"))) {
					migrateEdge(edge1, "HAS_INITIAL_BRANCH", true);
				}

				for (Edge edge1 : StreamUtil.toIterable(in.edges(OUT, "HAS_LATEST_RELEASE"))) {
					migrateEdge(edge1, "HAS_LATEST_BRANCH", true);
				}
			}

		}

		migrateJobProperties();
	}

	private void migrateJobProperties() {
		Vertex meshRoot = getMeshRootVertex();
		for (Vertex root : StreamUtil.toIterable(meshRoot.vertices(OUT, "HAS_JOB_ROOT"))) {
			for (Vertex job : StreamUtil.toIterable(root.vertices(OUT, "HAS_JOB"))) {
				migrateProperty(job, "releaseName", "branchName");
				migrateProperty(job, "releaseUuid", "branchUuid");
			}
		}
	}

	private boolean migrateBranchEdgeProperties(String label, Queue<Object> jobQueue, long limit) {
		long n = 0;
		while (!jobQueue.isEmpty()) {
			Object id = jobQueue.poll();
			Edge edge = getGraph().edges(id).next();
			migrateProperty(edge, "releaseUuid", "branchUuid");
			n++;
			double percent = 1;
			if (jobQueue.size() != 0) {
				percent = (double) n / (double) jobQueue.size();
			}
			if (n % 8_000 == 0) {
				log.info("Migrated {" + n + "} " + label + " edges. Remaining " + jobQueue.size() + " (" + (percent * 100) + "%)");
				getGraph().tx().commit();
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
		long count = 0;
		try (DefaultGraphTraversal<?, Vertex> t = new DefaultGraphTraversal<>(getGraph())) {
			Iterable<Vertex> it = () -> t.has(ElementFrame.TYPE_RESOLUTION_KEY, from);
			for (Vertex fromV : it) {
				// Create new vertex with new type
				Vertex toV = getGraph().addVertex("class:" + to);
				// Duplicate the in edges
				for (Edge inE : StreamUtil.toIterable(fromV.edges(Direction.IN))) {
					Vertex out = inE.outVertex();
					Edge e = out.addEdge(inE.label(), toV);
					for (Property<Object> property : ((Iterable<Property<Object>>)() -> inE.properties())) {
						e.property(property.key(), inE.property(property.key()));
					}
				}
				// Duplicate the out edges
				for (Edge outE : StreamUtil.toIterable(fromV.edges(Direction.OUT))) {
					Vertex in = outE.inVertex();
					Edge e = toV.addEdge(outE.label(), in);
					for (Property<Object> property : ((Iterable<Property<Object>>)() -> outE.properties())) {
						e.property(property.key(), outE.property(property.key()));
					}
				}
				// Duplicate properties
				for (VertexProperty<Object> property : ((Iterable<VertexProperty<Object>>)() -> fromV.properties())) {
					toV.property(property.key(), fromV.property(property.key() ));
				}

				// Update the ferma type
				toV.property("ferma_type", to);
				fromV.remove();
				count++;
				if (count % 1000 == 0) {
					log.info("Migrated {" + count + "} vertices.");
					getGraph().tx().commit();
				}
				if (count >= limit) {
					log.info("Limit for batch reached");
					return true;
				}
			}
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}
		
		log.info("Migrated total of {" + count + "} vertices from {" + from + "} to {" + to + "}");
		if (count == 0) {
			return false;
		}
		return true;
	}

	private void migrateProperty(Vertex vertex, String oldPropertyKey, String newPropertyKey) {
		log.info("Migrating vertex: " + vertex.id());
		VertexProperty<String> property = vertex.property(oldPropertyKey);
		String value = property.orElse(null);
		property.remove();
		if (value != null) {
			vertex.property(newPropertyKey, value);
		}
	}

	private void migrateProperty(Edge edge, String oldPropertyKey, String newPropertyKey) {
		Property<String> property = edge.property(oldPropertyKey);
		String value = property.orElse(null);
		property.remove();
		if (value != null) {
			edge.property(newPropertyKey, value);
		}
	}

	// private Vertex migrateType(Vertex vertex, String newType) {
	// String type = vertex.property("ferma_type");
	// vertex.property("ferma_type", newType);
	// Vertex newVertex = getDb().changeType(vertex, newType, getGraph());
	// log.info("Migrating {" + type + "} to {" + newType + "}");
	// return newVertex;
	// }

	private void migrateEdge(Edge edge, String newLabel, boolean reverseOrder) {
		Vertex in = edge.inVertex();
		Vertex out = edge.outVertex();
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
