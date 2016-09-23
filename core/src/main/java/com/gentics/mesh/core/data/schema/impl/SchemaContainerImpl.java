package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see SchemaContainer
 */
public class SchemaContainerImpl extends AbstractGraphFieldSchemaContainer<Schema, SchemaReference, SchemaContainer, SchemaContainerVersion>
		implements SchemaContainer {

	@Override
	protected Class<? extends SchemaContainer> getContainerClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	protected Class<? extends SchemaContainerVersion> getContainerVersionClass() {
		return SchemaContainerVersionImpl.class;
	}

	public static void init(Database database) {
		database.addVertexType(SchemaContainerImpl.class, MeshVertexImpl.class);
	}

	@Override
	public SchemaReference createEmptyReferenceModel() {
		return new SchemaReference();
	}

	@Override
	public RootVertex<SchemaContainer> getRoot() {
		return MeshRoot.getInstance().getSchemaContainerRoot();
	}

	@Override
	public String getType() {
		return SchemaContainer.TYPE;
	}

	@Override
	public List<? extends NodeImpl> getNodes() {
		return in(HAS_SCHEMA_CONTAINER).toListExplicit(NodeImpl.class);
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		List<? extends NodeImpl> list = getNodes();
		if (list.isEmpty()) {
			batch.addEntry(this, DELETE_ACTION);
			getElement().remove();
			//TODO handel related elements?
		} else {
			throw error(BAD_REQUEST, "schema_delete_still_in_use", getUuid());
		}
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return "/api/v1/schemas/" + getUuid();
	}

}
