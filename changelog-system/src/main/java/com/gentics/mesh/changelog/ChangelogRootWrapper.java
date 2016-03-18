package com.gentics.mesh.changelog;

import java.util.Objects;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

/**
 * Simple tinkerpop wrapper for the found vertex which represents the changelog root.
 */
public class ChangelogRootWrapper {

	public static final String HAS_CHANGELOG_ROOT = "HAS_CHANGELOG_ROOT";
	public static final String HAS_CHANGE = "HAS_CHANGE";

	private Vertex rootVertex;
	private TransactionalGraph graph;

	public ChangelogRootWrapper(TransactionalGraph graph, Vertex vertex) {
		this.graph = graph;
		this.rootVertex = vertex;
	}

	public boolean hasChange(String uuid) {
		Objects.requireNonNull(uuid, "The uuid of the change must not be null");
		for (Vertex vertex : rootVertex.getVertices(Direction.OUT, HAS_CHANGE)) {
			ChangeWrapper change = new ChangeWrapper(vertex);
			if (uuid.equals(change.getUuid())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add the change to the list of executed changes.
	 * 
	 * @param abstractChange
	 */
	public void add(Change change) {
		Vertex vertex = graph.addVertex(ChangeWrapper.class);
		ChangeWrapper graphChange = new ChangeWrapper(vertex);
		graphChange.update(change);
		rootVertex.addEdge(HAS_CHANGE, vertex);
	}
}
