package com.gentics.mesh.changelog.changes;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;

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
		Vertex projectRoot = meshRoot.vertices(Direction.OUT, "HAS_PROJECT_ROOT").next();

		// Iterate over all projects
		for (Vertex project : (Iterable<Vertex>) () -> projectRoot.vertices(Direction.OUT, "HAS_PROJECT")) {
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
		Iterator<Edge> edges = node.edges(Direction.OUT, "HAS_FIELD_CONTAINER");
		Edge referenceEdge = null;
		Vertex possibleDraftContainer = null;
		// Determine whether a draft edge exists and locate the publish edge.
		// The publish edge will be copied to create the missing draft edge.
		for (Edge edge : (Iterable<Edge>) () -> edges) {
			String type = edge.value("edgeType");
			if ("D".equals(type)) {
				foundDraft = true;
			}
			if ("P".equals(type)) {
				referenceEdge = edge;
			}

			// Only one field container can have the webroot path info set
			Vertex fieldContainer = edge.inVertex();
			String pathInfo = fieldContainer.value("webrootPathInfo");
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
			draftEdge.property("branchUuid", referenceEdge.property("branchUuid"));
			draftEdge.property("edgeType", "D");
			draftEdge.property("languageTag", referenceEdge.property("languageTag"));
		}

		// Now check the children and migrate structure
		Iterator<Edge> childrenEdges = node.edges(Direction.IN, "HAS_PARENT_NODE");
		for (Edge childEdge : (Iterable<Edge>) () -> childrenEdges) {
			migrateNode(childEdge.outVertex());
		}

	}

	@Override
	public String getUuid() {
		return "72A8F88032A24E24A8F88032A2CE2403";
	}

}