package com.gentics.mesh.changelog;

import java.util.Objects;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.madl.tx.Tx;

/**
 * Simple tinkerpop wrapper for the found vertex which represents the changelog root.
 */
public class ChangelogRootWrapper {

	public static final String HAS_CHANGELOG_ROOT = "HAS_CHANGELOG_ROOT";
	public static final String HAS_CHANGE = "HAS_CHANGE";

	private Vertex rootVertex;
	private Tx tx;

	public ChangelogRootWrapper(Tx tx, Vertex vertex) {
		this.tx = tx;
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
		for (Vertex vertex : (Iterable<Vertex>) () -> rootVertex.vertices(Direction.OUT, HAS_CHANGE)) {
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
		Vertex vertex = tx.addVertex(ChangeWrapper.class);
		ChangeWrapper graphChange = new ChangeWrapper(vertex);
		graphChange.update(change);
		rootVertex.addEdge(HAS_CHANGE, vertex);
	}
}
