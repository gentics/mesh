package com.gentics.mesh.core.data.search.impl;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.index.IndexHandler;

import rx.Observable;

/**
 * @see SearchQueueEntry
 */
public class SearchQueueEntryImpl extends MeshVertexImpl implements SearchQueueEntry {

	private static final String ACTION_KEY = "element_action";
	private static final String ELEMENT_UUID = "element_uuid";
	private static final String ELEMENT_TYPE = "element_type";
	private static final String ELEMENT_INDEX_TYPE = "element_index_type";

	private static final String CUSTOM_PREFIX = "custom_";

	public static void checkIndices(Database database) {
		database.addVertexType(SearchQueueEntryImpl.class);
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
		String actionName = getProperty(ACTION_KEY);
		return actionName;
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

	@Override
	public String getElementIndexType() {
		return getProperty(ELEMENT_INDEX_TYPE);
	}

	@Override
	public SearchQueueEntry setElementIndexType(String indexType) {
		setProperty(ELEMENT_INDEX_TYPE, indexType);
		return this;
	}

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
	public Observable<Void> process() {
		return getIndexHandler(getElementType()).handleAction(this);
	}

	@Override
	public String toString() {
		return "uuid: " + getElementUuid() + " type: " + getElementType() + " action: " + getElementActionName();
	}

	@Override
	public <T> T getCustomProperty(String name) {
		return getProperty(CUSTOM_PREFIX + name);
	}

	@Override
	public void setCustomProperty(String name, Object value) {
		setProperty(CUSTOM_PREFIX + name, value);
	}
}
