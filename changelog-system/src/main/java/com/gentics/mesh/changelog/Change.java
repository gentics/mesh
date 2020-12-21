package com.gentics.mesh.changelog;

import com.gentics.mesh.graphdb.spi.Database;
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

	/**
	 * Validate the change. A unsuccessful validation will abort the changelog execution.
	 * 
	 * @return true if validation was successful. Otherwise false.
	 */
	boolean validate();

	/**
	 * Return a flag which informs the changelog system whether the change requires a reindex of the search index.
	 * 
	 * @return
	 */
	boolean requiresReindex();

	/**
	 * Set the database.
	 * 
	 * @param db
	 */
	void setDb(Database db);

	/**
	 * Return the database.
	 * 
	 * @return
	 */
	Database getDb();

}
