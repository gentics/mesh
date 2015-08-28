package com.gentics.mesh.core.data.search.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;

import java.util.List;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;

public class SearchQueueBatchImpl extends MeshVertexImpl implements SearchQueueBatch {

	@Override
	public void addEntry(String uuid, String type, SearchQueueEntryAction action) {
		SearchQueueEntry entry = getGraph().addFramedVertex(SearchQueueEntryImpl.class);
		entry.setElementUuid(uuid);
		entry.setElementType(type);
		entry.setAction(action.getName());
		addEntry(entry);
	}

	@Override
	public void addEntry(SearchQueueEntry batch) {
		setLinkOutTo(batch.getImpl(), HAS_ITEM);
	}

	@Override
	public void addEntry(GenericVertex<?> vertex, SearchQueueEntryAction action) {
		addEntry(vertex.getUuid(), vertex.getType(), action);
	}

	@Override
	public List<? extends SearchQueueEntry> getEntries() {
		return out(HAS_ITEM).has(SearchQueueEntryImpl.class).toListExplicit(SearchQueueEntryImpl.class);
	}

	@Override
	public String getBatchId() {
		return getProperty(BATCH_ID_PROPERTY_KEY);
	}

	@Override
	public void setBatchId(String batchId) {
		setProperty(BATCH_ID_PROPERTY_KEY, batchId);
	}
	
	@Override
	public void delete() {
		for(SearchQueueEntry entry : getEntries()) {
			entry.delete();
		}
		getVertex().remove();
	}

}
