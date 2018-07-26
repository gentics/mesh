package com.gentics.mesh.core.data.changelog;

public interface Change {

	/**
	 * Return the uuid of the change.
	 * 
	 * @return
	 */
	String getUuid();

	/**
	 * Return the name of the change.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Apply the change to the graph.
	 */
	void apply();

	/**
	 * Return the description of the change.
	 * 
	 * @return
	 */
	String getDescription();

}
