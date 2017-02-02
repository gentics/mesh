package com.gentics.mesh.core.data.search;

/**
 * Batch entry which contains information for storage or deletion elements from the search index.
 */
public interface UpdateDocumentEntry extends SearchQueueEntry {

	/**
	 * Return the search queue entry element uuid which identifies the element that should be handled.
	 * 
	 * @return
	 */
	String getElementUuid();

}
