package com.gentics.mesh.changelog;

import com.tinkerpop.blueprints.TransactionalGraph;

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

	/**
	 * Set the graph instance to be used when handling the change.
	 * 
	 * @param graph
	 */
	void setGraph(TransactionalGraph graph);

	/**
	 * Return the graph that is currently set for the change.
	 * 
	 * @return
	 */
	TransactionalGraph getGraph();

	/**
	 * Persist the info that the change has been applied to the graph.
	 */
	void markAsComplete();

	/**
	 * Set the time that passed to execute the change.
	 * 
	 * @param timeMs
	 */
	void setDuration(long timeMs);

	/**
	 * Return the time it took to execute the change.
	 * 
	 * @return
	 */
	long getDuration();

}
