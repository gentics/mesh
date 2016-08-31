package com.gentics.mesh.core.data.search.impl;

import java.util.Map;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.index.IndexHandler;

import rx.Completable;

/**
 * @see SearchQueueEntry
 */
public class SearchQueueEntryImpl extends MeshVertexImpl implements SearchQueueEntry, Comparable<SearchQueueEntry> {

	public static final String ACTION_KEY = "element_action";
	public static final String ELEMENT_UUID = "element_uuid";
	public static final String ELEMENT_TYPE = "element_type";
	public static final String ELEMENT_INDEX_TYPE = "element_index_type";
	public static final String ENTRY_TIME = "entry_time";

	private static final String CUSTOM_PREFIX = "custom_";

	public static void init(Database database) {
		database.addVertexType(SearchQueueEntryImpl.class, MeshVertexImpl.class);
	}

	@Override
	public String getElementUuid() {
		return getProperty(ELEMENT_UUID);
	}

	@Override
	public SearchQueueEntry setElementUuid(String uuid) {
		setProperty(ELEMENT_UUID, uuid);
		return this;
	}

	@Override
	public SearchQueueEntryAction getElementAction() {
		return SearchQueueEntryAction.valueOfName(getElementActionName());
	}

	@Override
	public String getElementActionName() {
		return getProperty(ACTION_KEY);
	}

	@Override
	public SearchQueueEntry setElementAction(String action) {
		setProperty(ACTION_KEY, action);
		return this;
	}

	@Override
	public String getElementType() {
		return getProperty(ELEMENT_TYPE);
	}

	@Override
	public SearchQueueEntry setElementType(String type) {
		setProperty(ELEMENT_TYPE, type);
		return this;
	}

	//	@Override
	//	public String getElementIndexType() {
	//		return getProperty(ELEMENT_INDEX_TYPE);
	//	}
	//
	//	@Override
	//	public SearchQueueEntry setElementIndexType(String indexType) {
	//		setProperty(ELEMENT_INDEX_TYPE, indexType);
	//		return this;
	//	}

	@Override
	public void delete(SearchQueueBatch batch) {
		getVertex().remove();
	}

	/**
	 * Return the index handler for the given type.
	 * 
	 * @param type
	 * @return
	 */
	public IndexHandler getIndexHandler(String type) {
		return IndexHandlerRegistry.getInstance().get(type);
	}

	@Override
	public Completable process() {
		return getIndexHandler(getElementType()).handleAction(this);
	}

	@Override
	public String toString() {
		return "uuid: " + getElementUuid() + " type: " + getElementType() + " action: " + getElementActionName();
	}

	@Override
	public <T> T get(String name) {
		return getProperty(CUSTOM_PREFIX + name);
	}

	@Override
	public SearchQueueEntry set(String name, Object value) {
		setProperty(CUSTOM_PREFIX + name, value);
		return this;
	}

	@Override
	public SearchQueueEntry set(Map<String, Object> properties) {
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			setProperty(entry.getKey(), entry.getValue());
		}
		return this;
	}

	@Override
	public SearchQueueEntry setTime(long timeInMs) {
		setProperty(ENTRY_TIME, timeInMs);
		return this;
	}

	@Override
	public long getTime() {
		return getProperty(ENTRY_TIME);
	}

	@Override
	public int compareTo(SearchQueueEntry o) {
		return getElementAction().getOrder().compareTo(o.getElementAction().getOrder());
	}

	@Override
	public void delete() {
		getElement().remove();
	}

}
