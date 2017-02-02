package com.gentics.mesh.core.data.search;

/**
 * Entry which instructs the index handler to create the index using the provided information.
 */
public interface CreateIndexEntry extends SearchQueueEntry {

	/**
	 * Name of the index which should be created.
	 * 
	 * @return
	 */
	String getIndexName();

	/**
	 * Type of the index which should be created.
	 * 
	 * @return
	 */
	String getIndexType();

}
