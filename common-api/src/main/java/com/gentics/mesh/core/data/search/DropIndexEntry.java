package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.search.context.GenericEntryContext;

/**
 * Entry which instructs the index handler to drop the index using the provided information.
 */
public interface DropIndexEntry extends SeperateSearchQueueEntry<GenericEntryContext> {

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
