package com.gentics.mesh.changelog.changes;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.util.StreamUtil;

/**
 * Changelog entry which corrects the graph.
 */
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
	public void applyInTx() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex projectRoot = meshRoot.vertices(Direction.OUT, "HAS_PROJECT_ROOT").next();

		// Iterate over all projects
		for (Vertex project : StreamUtil.toIterable(projectRoot.vertices(Direction.OUT, "HAS_PROJECT"))) {
			// Migrate all nodes of the project
			Vertex baseNode = project.vertices(Direction.OUT, "HAS_ROOT_NODE").next();
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
		Iterable<Edge> edges = StreamUtil.toIterable(node.edges(Direction.OUT, "HAS_FIELD_CONTAINER"));
		Edge referenceEdge = null;
		Vertex possibleDraftContainer = null;
		// Determine whether a draft edge exists and locate the publish edge.
		// The publish edge will be copied to create the missing draft edge.
		for (Edge edge : edges) {
			String type = edge.<String>property("edgeType").orElse(null);
			if ("D".equals(type)) {
				foundDraft = true;
			}
			if ("P".equals(type)) {
				referenceEdge = edge;
			}

			// Only one field container can have the webroot path info set
			Vertex fieldContainer = edge.inVertex();
			String pathInfo = fieldContainer.<String>property("webrootPathInfo").orElse(null);
			if (pathInfo != null) {
				referenceEdge = edge;
				possibleDraftContainer = fieldContainer;
			}
		}

		if (!foundDraft) {
			// Check which container should become the new draft. We may have found the original draft container. Use it if possible
			Vertex fieldContainer = possibleDraftContainer;
			if (fieldContainer == null) {
				fieldContainer = referenceEdge.inVertex();
			}
			Edge draftEdge = node.addEdge("HAS_FIELD_CONTAINER", fieldContainer);
			draftEdge.property("ferma_type", "GraphFieldContainerEdgeImpl");
			draftEdge.property("branchUuid", referenceEdge.<String>property("branchUuid").orElse(null));
			draftEdge.property("edgeType", "D");
			draftEdge.property("languageTag", referenceEdge.<String>property("languageTag").orElse(null));
		}

		// Now check the children and migrate structure
		Iterable<Edge> childrenEdges = StreamUtil.toIterable(node.edges(Direction.IN, "HAS_PARENT_NODE"));
		for (Edge childEdge : childrenEdges) {
			migrateNode(childEdge.outVertex());
		}

	}

	@Override
	public String getUuid() {
		return "72A8F88032A24E24A8F88032A2CE2403";
	}

}