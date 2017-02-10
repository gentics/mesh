package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see SchemaContainer
 */
public class SchemaContainerImpl extends AbstractGraphFieldSchemaContainer<SchemaResponse, Schema, SchemaReference, SchemaContainer, SchemaContainerVersion>
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
	public SchemaReference transformToReference() {
		return new SchemaReference().setName(getName()).setUuid(getUuid());
	}

	@Override
	public RootVertex<SchemaContainer> getRoot() {
		return MeshInternal.get().boot().meshRoot().getSchemaContainerRoot();
	}

	@Override
	public List<? extends NodeImpl> getNodes() {
		return in(HAS_SCHEMA_CONTAINER).toListExplicit(NodeImpl.class);
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		// Check whether the schema is currently being referenced by nodes.
		List<? extends NodeImpl> list = getNodes();
		if (list.isEmpty()) {
			batch.delete(this, true);
			getElement().remove();
			// TODO handel related elements?
		} else {
			throw error(BAD_REQUEST, "schema_delete_still_in_use", getUuid());
		}
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return "/api/v1/schemas/" + getUuid();
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public User getEditor() {
		return out(HAS_EDITOR).nextOrDefaultExplicit(UserImpl.class, null);
	}

}
