package com.gentics.mesh.changelog;

import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Wrapper object which wraps a change vertex in order to provide getter and setters for various vertex properties.
 */
public class ChangeWrapper {

	private Vertex vertex;

	/**
	 * Create a new wrapped change using the given vertex.
	 * 
	 * @param vertex
	 */
	public ChangeWrapper(Vertex vertex) {
		this.vertex = vertex;
	}

	/**
	 * Return the change uuid.
	 * 
	 * @return
	 */
	public String getUuid() {
		return vertex.value("uuid");
	}

	/**
	 * Update the graph vertex which represents a change using the provided change as a template.
	 * 
	 * @param change
	 */
	public void update(Change change) {
		setUuid(change.getUuid());
		setDuration(change.getDuration());
	}

	/**
	 * Set the execution duration for the change.
	 * 
	 * @param duration
	 */
	private void setDuration(long duration) {
		vertex.property("duration", duration);
	}

	/**
	 * Set the change uuid.
	 * 
	 * @param uuid
	 */
	private void setUuid(String uuid) {
		vertex.property("uuid", uuid);
	}
}
