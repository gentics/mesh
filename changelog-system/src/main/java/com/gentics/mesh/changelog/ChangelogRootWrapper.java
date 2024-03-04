package com.gentics.mesh.changelog;

import java.util.Objects;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.madl.frame.ElementFrame;

/**
 * Simple tinkerpop wrapper for the found vertex which represents the changelog root.
 */
public class ChangelogRootWrapper {

	public static final String HAS_CHANGELOG_ROOT = "HAS_CHANGELOG_ROOT";
	public static final String HAS_CHANGE = "HAS_CHANGE";

	private Vertex rootVertex;
	private Graph graph;

	public ChangelogRootWrapper(Graph graph, Vertex vertex) {
		this.graph = graph;
		this.rootVertex = vertex;
	}

	/**
	 * Check whether a change with the given UUID is already stored in the graph.
	 * 
	 * @param uuid
	 * @return <tt>true</tt> if the change is already stored within the changelog root
	 */
	public boolean hasChange(String uuid) {
		Objects.requireNonNull(uuid, "The uuid of the change must not be null");
		Iterable<Vertex> vertices = () -> rootVertex.vertices(Direction.OUT, HAS_CHANGE);
		for (Vertex vertex : vertices) {
			ChangeWrapper change = new ChangeWrapper(vertex);
			if (uuid.equals(change.getUuid())) {
				return true;
			}
			// Backport handling for legacy changelog entries
			// TODO write a changelog entry to clearup the existing changelog entries and remove this check
			String legacyUuid = "com.gentics.mesh.changelog.changes." + uuid;
			if (legacyUuid.equals(change.getUuid())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add the change to the list of executed changes.
	 * 
	 * @param change
	 */
	public void add(Change change) {
		Vertex vertex = graph.addVertex(ChangeWrapper.class.getSimpleName())
				.property(ElementFrame.TYPE_RESOLUTION_KEY, ChangeWrapper.class.getSimpleName()).element();
		ChangeWrapper graphChange = new ChangeWrapper(vertex);
		graphChange.update(change);
		rootVertex.addEdge(HAS_CHANGE, vertex);
	}
}
