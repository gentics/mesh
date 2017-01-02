package com.gentics.mesh.core.data.search.impl;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.search.index.IndexHandler;

import rx.Completable;

/**
 * @see SearchQueueEntry
 */
public class SearchQueueEntryImpl implements SearchQueueEntry, Comparable<SearchQueueEntry> {

	private String elementUuid;
	private String elementType;
	private SearchQueueEntryAction elementAction;
	private String elementIndexType;
	private long timestamp;
	private Map<String, Object> properties = new HashMap<>();

	@Override
	public String getElementUuid() {
		return elementUuid;
	}

	@Override
	public SearchQueueEntry setElementUuid(String uuid) {
		this.elementUuid = uuid;
		return this;
	}

	@Override
	public SearchQueueEntryAction getElementAction() {
		return elementAction;
	}

	@Override
	public SearchQueueEntry setElementAction(SearchQueueEntryAction action) {
		this.elementAction = action;
		return this;
	}

	@Override
	public String getElementType() {
		return elementType;
	}

	@Override
	public SearchQueueEntry setElementType(String type) {
		this.elementType = type;
		return this;
	}

	//	@Override
	//	public String getElementIndexType() {
	//		return getProperty(ELEMENT_INDEX_TYPE);
	//	}
	//
	//	@Overridefor (Map.Entry<String, Object> entry : properties.entrySet()) {
	//	setProperty(entry.getKey(), entry.getValue());}

	//	public SearchQueueEntry setElementIndexType(String indexType) {
	//		setProperty(ELEMENT_INDEX_TYPE, indexType);
	//		return this;
	//	}

	/**
	 * Return the index handler for the given type.
	 * 
	 * @param type
	 * @return
	 */
	public IndexHandler getIndexHandler(String type) {
		return MeshInternal.get().indexHandlerRegistry().getHandlerWithKey(type);
	}

	@Override
	public Completable process() {
		return getIndexHandler(getElementType()).handleAction(this);
	}

	@Override
	public String toString() {
		return "uuid: {" + getElementUuid() + "} type: {" + getElementType() + "} action: {" + getElementAction().name() + "}";
	}

	@Override
	public <T> T get(String key) {
		return (T) properties.get(key);
	}

	@Override
	public SearchQueueEntry set(String name, Object value) {
		this.properties.put(name, value);
		return this;
	}

	@Override
	public SearchQueueEntry set(Map<String, Object> properties) {
		this.properties.putAll(properties);
		return this;
	}

	@Override
	public SearchQueueEntry setTime(long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	@Override
	public long getTime() {
		return timestamp;
	}

	@Override
	public int compareTo(SearchQueueEntry o) {
		return getElementAction().getOrder().compareTo(o.getElementAction().getOrder());
	}

}
