package com.gentics.mesh.core.data.search.impl;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.action.ActionResponse;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.search.index.AbstractIndexHandler;
import com.gentics.mesh.search.index.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.search.index.ProjectIndexHandler;
import com.gentics.mesh.search.index.SchemaContainerIndexHandler;
import com.gentics.mesh.search.index.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.TagIndexHandler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class SearchQueueEntryImpl extends MeshVertexImpl implements SearchQueueEntry {

	private static final String ACTION_KEY = "element_action";
	private static final String ELEMENT_UUID = "element_uuid";
	private static final String ELEMENT_TYPE = "element_type";

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
		message.put("action", getElementAction().getName());
		return message;
	}

	@Override
	public void delete() {
		getVertex().remove();
	}

	public AbstractIndexHandler<?> getIndexHandler(String type) {
		// TODO i think it would be better to register handlers at one point and just use an abstract implementation to access the correct handler.
		switch (type) {
		case Tag.TYPE:
			return TagIndexHandler.getInstance();
		case TagFamily.TYPE:
			return TagFamilyIndexHandler.getInstance();
		case Node.TYPE:
			return NodeIndexHandler.getInstance();
		case Project.TYPE:
			return ProjectIndexHandler.getInstance();
		case SchemaContainer.TYPE:
			return SchemaContainerIndexHandler.getInstance();
		case MicroschemaContainer.TYPE:
			return MicroschemaContainerIndexHandler.getInstance();
		default:
			throw new NotImplementedException("Index type {" + type + "} is not implemented.");
		}

	}

	@Override
	public void process(Handler<AsyncResult<ActionResponse>> handler) {
		getIndexHandler(getElementType()).handleAction(getElementUuid(), getElementActionName(), handler);
	}

}
