package com.gentics.mesh.core.data.search.impl;

import io.vertx.core.json.JsonObject;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;

public class SearchQueueEntryImpl extends MeshVertexImpl implements SearchQueueEntry {

	private static final String ACTION_KEY = "action";
	private static final String ELEMENT_UUID = "element_uuid";
	private static final String ELEMENT_TYPE = "element_type";

	@Override
	public SearchQueueEntryAction getAction() {
		String actionName = getProperty(ACTION_KEY);
		return SearchQueueEntryAction.valueOfName(actionName);
	}

	@Override
	public SearchQueueEntry setAction(String action) {
		setProperty(ACTION_KEY, action);
		return this;
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
	public String getElementType() {
		return getProperty(ELEMENT_TYPE);
	}

	@Override
	public SearchQueueEntry setElementType(String type) {
		setProperty(ELEMENT_TYPE, type);
		return this;
	}

	@Override
	public JsonObject getMessage() {
		JsonObject message = new JsonObject();
		message.put("uuid", getElementUuid());
		message.put("type", getElementType());
		message.put("action", getAction().getName());
		return message;
	}

	@Override
	public void delete() {
		getVertex().remove();
	}

}
