package com.gentics.mesh.changelog.changes;

import static com.tinkerpop.blueprints.Direction.IN;
import static com.tinkerpop.blueprints.Direction.OUT;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

public class CreateMissingDraftEdges extends AbstractChange {

	@Override
	public String getName() {
		return "Create missing draft edges";
	}

	@Override
	public String getDescription() {
		return "Older instances are missing a few draft edges. This change recreates those edges.";
	}

	@Override
	public void apply() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex projectRoot = meshRoot.getVertices(Direction.OUT, "HAS_PROJECT_ROOT").iterator().next();

		// Iterate over all projects
		for (Vertex project : projectRoot.getVertices(Direction.OUT, "HAS_PROJECT")) {
			// Migrate all nodes of the project
			Vertex baseNode = project.getVertices(Direction.OUT, "HAS_ROOT_NODE").iterator().next();
			migrateNode(baseNode);
		}
	}

	/**
	 * Create missing draft edges
	 * 
	 * @param node
	 */
	private void migrateNode(Vertex node) {

		boolean foundDraft = false;
		Iterable<Edge> edges = node.getEdges(Direction.OUT, "HAS_FIELD_CONTAINER");
		Edge publishEdge = null;

		// Determine whether a draft edge exists and locate the publish edge. The publish edge will be copied to create the missing draft edge.
		for (Edge edge : edges) {
			String type = edge.getProperty("edgeType");
			if ("D".equals(type)) {
				foundDraft = true;
			}
			if ("P".equals(type)) {
				publishEdge = edge;
			}
		}

		if (!foundDraft) {
			Vertex publishFieldContainer = publishEdge.getVertex(IN);
			Edge draftEdge = node.addEdge("HAS_FIELD_CONTAINER", publishFieldContainer);
			draftEdge.setProperty("ferma_type", "GraphFieldContainerEdgeImpl");
			draftEdge.setProperty("releaseUuid", publishEdge.getProperty("releaseUuid"));
			draftEdge.setProperty("edgeType", "D");
			draftEdge.setProperty("languageTag", publishEdge.getProperty("languageTag"));
		}

		// Now check the children and migrate structure
		Iterable<Edge> childrenEdges = node.getEdges(Direction.IN, "HAS_PARENT_NODE");
		for (Edge childEdge : childrenEdges) {
			migrateNode(childEdge.getVertex(Direction.OUT));
		}

	}

	@Override
	public String getUuid() {
		return "72A8F88032A24E24A8F88032A2CE2403";
	}

}