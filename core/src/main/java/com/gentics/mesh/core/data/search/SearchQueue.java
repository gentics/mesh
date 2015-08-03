package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.MeshVertex;

public interface SearchQueue extends MeshVertex {
	
	public static final String SEARCH_QUEUE_ENTRY_ADDRESS = "search-queue-entry";

	void put(String uuid, String type, SearchQueueEntryAction action);

	void put(SearchQueueEntry entry);

	SearchQueueEntry take() throws InterruptedException;

	long getSize();

}
