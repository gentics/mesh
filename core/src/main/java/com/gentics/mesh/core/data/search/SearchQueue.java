package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.MeshVertex;

public interface SearchQueue extends MeshVertex {

	void put(String uuid, String type, SearchQueueEntryAction action);

	void put(SearchQueueEntry entry);

	SearchQueueEntry take() throws InterruptedException;

	long getSize();

}
