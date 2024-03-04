package com.gentics.mesh.changelog.changes;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.PROJECT_KEY_PROPERTY;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.util.StreamUtil;

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
	public void applyInTx() throws Exception {
		try (GraphTraversal<Vertex, Vertex> t = getGraph().traversal().V()) {
			iterateWithCommit(StreamUtil.toIterable(t.hasLabel( "NodeImpl")), vertex -> {
				vertex.edges(Direction.IN, "HAS_NODE").forEachRemaining(Edge::remove);
				replaceSingleEdge(vertex, Direction.OUT, "ASSIGNED_TO_PROJECT", PROJECT_KEY_PROPERTY);
			});
		}
	}
}
