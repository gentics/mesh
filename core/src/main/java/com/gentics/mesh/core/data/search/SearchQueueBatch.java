package com.gentics.mesh.core.data.search;

import java.util.List;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshVertex;

public interface SearchQueueBatch extends MeshVertex {

	void addEntry(String uuid, String type, SearchQueueEntryAction action);

	void addEntry(GenericVertex<?> vertex, SearchQueueEntryAction action);

	void addEntry(SearchQueueEntry entry);

	List<? extends SearchQueueEntry> getEntries();

}
