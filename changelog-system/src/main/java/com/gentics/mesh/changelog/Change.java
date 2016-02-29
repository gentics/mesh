package com.gentics.mesh.changelog;

/**
 * Interface for a mesh graph database change. A change may alter graph structure, content and indices.
 */
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
	 * Check whether the change already has been applied.
	 * 
	 * @return
	 */
	boolean isApplied();

	/**
	 * Return the description of the change.
	 * 
	 * @return
	 */
	String getDescription();

	/**
	 * Return a flag that indicates whether the change should force a rebuild of the search index after all changes have been applied.
	 * 
	 * @return
	 */
	boolean doesForceReindex();

}
