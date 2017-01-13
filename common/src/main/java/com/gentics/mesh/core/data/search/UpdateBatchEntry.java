package com.gentics.mesh.core.data.search;

public interface UpdateBatchEntry extends SearchQueueEntry {

	/**
	 * Return the search queue entry element uuid which identifies the element that should be handled.
	 * 
	 * @return
	 */
	String getElementUuid();

}
