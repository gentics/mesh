package com.gentics.mesh.core.data.search.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.index.AbstractIndexHandler;
import com.gentics.mesh.search.index.GroupIndexHandler;
import com.gentics.mesh.search.index.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.search.index.ProjectIndexHandler;
import com.gentics.mesh.search.index.RoleIndexHandler;
import com.gentics.mesh.search.index.SchemaContainerIndexHandler;
import com.gentics.mesh.search.index.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.TagIndexHandler;
import com.gentics.mesh.search.index.UserIndexHandler;

import rx.Observable;

/**
 * @see SearchQueueEntry
 */
public class SearchQueueEntryImpl extends MeshVertexImpl implements SearchQueueEntry {

	private static final String ACTION_KEY = "element_action";
	private static final String ELEMENT_UUID = "element_uuid";
	private static final String ELEMENT_TYPE = "element_type";
	private static final String ELEMENT_INDEX_TYPE = "element_index_type";

	public static void checkIndices(Database database) {
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
	public AbstractIndexHandler<?> getIndexHandler(String type) {
		// TODO i think it would be better to register handlers at one point and just use an 
		// abstract implementation to access the correct handler.
		switch (type) {
		case Node.TYPE:
			return NodeIndexHandler.getInstance();
		case Tag.TYPE:
			return TagIndexHandler.getInstance();
		case TagFamily.TYPE:
			return TagFamilyIndexHandler.getInstance();
		case User.TYPE:
			return UserIndexHandler.getInstance();
		case Group.TYPE:
			return GroupIndexHandler.getInstance();
		case Role.TYPE:
			return RoleIndexHandler.getInstance();
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
	public Observable<Void> process() {
		AbstractIndexHandler<?> indexHandler = getIndexHandler(getElementType());
		if (indexHandler == null) {
			//TODO i18n
			throw error(BAD_REQUEST, "No index handler could be found for type {" + getElementType() + "} of element {" + getElementUuid() + "}");
		}
		//TODO it would be possible to avoid loading by uuid for update and create requests. We should not reload the element by uuid.
		return indexHandler.handleAction(getElementUuid(), getElementActionName(), getElementIndexType());
	}

	@Override
	public String toString() {
		return "uuid: " + getElementUuid() + " type: " + getElementType() + " action: " + getElementActionName();
	}

}
