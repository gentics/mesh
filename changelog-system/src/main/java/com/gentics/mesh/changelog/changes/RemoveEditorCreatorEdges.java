package com.gentics.mesh.changelog.changes;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.CREATOR_UUID_PROPERTY_KEY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.EDITOR_UUID_PROPERTY_KEY;
import static com.tinkerpop.blueprints.Direction.IN;
import static com.tinkerpop.blueprints.Direction.OUT;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

/**
 * Change which removed the editor and creator edges from all elements.
 */
public class RemoveEditorCreatorEdges extends AbstractChange {

	@Override
	public String getUuid() {
		return "0B9DF9EDB94F473E9DF9EDB94F573EF3";
	}

	@Override
	public String getName() {
		return "RemoveEditorCreatorEdges";
	}

	@Override
	public String getDescription() {
		return "Replaces editor and creator edges from all vertices with a property containing the uuid.";
	}

	@Override
	public void applyInTx() {
		iterateWithCommit(getGraph().getVertices(), v -> {
			replaceUuidEdge(v, "HAS_CREATOR", CREATOR_UUID_PROPERTY_KEY);
			replaceUuidEdge(v, "HAS_EDITOR", EDITOR_UUID_PROPERTY_KEY);
		});
	}

	/**
	 * Replaces an edge to an element with a uuid with a property containing that uuid.
	 * @param vertex
	 * @param edgeLabel
	 * @param uuidPropertyKey
	 */
	private void replaceUuidEdge(Vertex vertex, String edgeLabel, String uuidPropertyKey) {
		for (Edge edge : vertex.getEdges(OUT, edgeLabel)) {
			Vertex reference = edge.getVertex(IN);
			vertex.setProperty(uuidPropertyKey, reference.getProperty("uuid"));
			edge.remove();
		}
	}
}
