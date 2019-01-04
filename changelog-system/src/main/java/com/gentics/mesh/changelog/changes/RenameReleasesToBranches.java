package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;

public class RenameReleasesToBranches extends AbstractChange {

	@Override
	public String getName() {
		return "ReleaseBranchRenameChange";
	}

	@Override
	public String getDescription() {
		return "Renames the release vertices, edges and properties to branch";
	}

	@Override
	public void apply() {
		getDb().addVertexType("BranchImpl", "MeshVertexImpl");
		getDb().addVertexType("BranchRootImpl", "MeshVertexImpl");
		getDb().addVertexType("BranchMigrationJobImpl", "MeshVertexImpl");

		Vertex meshRoot = getMeshRootVertex();

		Vertex projectRoot = meshRoot.vertices(OUT, "HAS_PROJECT_ROOT").next();
		for (Vertex project : (Iterable<Vertex>) () -> projectRoot.vertices(OUT, "HAS_PROJECT")) {

			for (Edge edge : (Iterable<Edge>) () -> project.edges(OUT, "HAS_RELEASE_ROOT")) {
				migrateEdge(edge, "HAS_BRANCH_ROOT", true);

				// Iterate over all releases
				Vertex in = edge.inVertex();
				migrateType(in, "BranchRootImpl");
				for (Edge edge1 : (Iterable<Edge>) () -> in.edges(OUT, "HAS_RELEASE")) {
					Vertex release = edge1.inVertex();
					migrateType(release, "BranchImpl");
					for (Edge nextReleaseEdge : (Iterable<Edge>) () -> release.edges(OUT, "HAS_NEXT_RELEASE")) {
						migrateEdge(nextReleaseEdge, "HAS_NEXT_BRANCH", true);
					}
					migrateEdge(edge1, "HAS_BRANCH", true);
				}

				for (Edge edge1 : (Iterable<Edge>) () -> in.edges(OUT, "HAS_INITIAL_RELEASE")) {
					migrateEdge(edge1, "HAS_INITIAL_BRANCH", true);
				}

				for (Edge edge1 : (Iterable<Edge>) () -> in.edges(OUT, "HAS_LATEST_RELEASE")) {
					migrateEdge(edge1, "HAS_LATEST_BRANCH", true);
				}
			}

		}

		long n = 0;
		for (Edge edge : tx.edges()) {
			String label = edge.label();
			boolean update = false;
			if (label.equals("HAS_TAG")) {
				migrateProperty(edge, label, "releaseUuid", "branchUuid");
				update = true;
			}
			if (label.equals("HAS_FIELD_CONTAINER")) {
				migrateProperty(edge, label, "releaseUuid", "branchUuid");
				update = true;
			}
			if (label.equals("HAS_PARENT_NODE")) {
				migrateProperty(edge, label, "releaseUuid", "branchUuid");
				update = true;
			}

			if (update) {
				n++;
				if (n % 100 == 0) {
					log.info("Migrated {" + n + "} edges");
				}
			}
		}
		log.info("Migrated {" + n + "} edges");

		// Migrate job properties
		for (Vertex root : (Iterable<Vertex>) () -> meshRoot.vertices(OUT, "HAS_JOB_ROOT")) {
			for (Vertex job : (Iterable<Vertex>) () -> root.vertices(OUT, "HAS_JOB")) {
				migrateProperty(job, "releaseName", "branchName");
				migrateProperty(job, "releaseUuid", "branchUuid");
				String type = job.value("ferma_type");
				if (type.equals("ReleaseMigrationJobImpl")) {
					migrateType(job, "BranchMigrationJobImpl");
				}
			}
		}

	}

	private void migrateProperty(Vertex vertex, String oldPropertyKey, String newPropertyKey) {
		log.info("Migrating vertex: " + vertex.id());
		String value = vertex.value(oldPropertyKey);
		vertex.property(oldPropertyKey).remove();
		if (value != null) {
			vertex.property(newPropertyKey, value);
		}
	}

	private void migrateProperty(Edge edge, String label, String oldPropertyKey, String newPropertyKey) {
		String value = edge.value(oldPropertyKey);
		edge.property(oldPropertyKey).remove();
		if (value != null) {
			edge.property(newPropertyKey, value);
		}
	}

	private void migrateType(Vertex vertex, String newType) {
		String type = vertex.value("ferma_type");
		vertex.property("ferma_type", newType);
		getDb().changeType(vertex, newType);
		log.info("Migrating {" + type + "} to {" + newType + "}");
	}

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
