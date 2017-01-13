package com.gentics.mesh.core.data.search;

/**
 * Entry which instructs the index handler to drop the index using the provided information.
 */
public interface DropIndexEntry extends SearchQueueEntry {

	/**
	 * Return the used index name for the referenced element.
	 * 
	 * @return
	 */
	String getIndexName();

	/**
	 * Return the index type for the referenced element.
	 * 
	 * @return
	 */
	String getIndexType();

}
