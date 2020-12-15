package com.gentics.mesh.changelog.changes;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.PROJECT_KEY_PROPERTY;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;

/**
 * Changelog entry which replaces the project edges.
 */
public class ReplaceProjectEdges extends AbstractChange {

	@Override
	public String getUuid() {
		return "4D718477D2714EF0B18477D2711EF0A4";
	}

	@Override
	public String getName() {
		return "ReplaceProjectEdges";
	}

	@Override
	public String getDescription() {
		return "Replaces edges from nodes to project and project node roots";
	}

	@Override
	public void applyInTx() {
		iterateWithCommit(getGraph().getVertices("@class", "NodeImpl"), vertex -> {
			vertex.getEdges(Direction.IN, "HAS_NODE").forEach(Edge::remove);
			replaceSingleEdge(vertex, Direction.OUT, "ASSIGNED_TO_PROJECT", PROJECT_KEY_PROPERTY);
		});
	}
}
