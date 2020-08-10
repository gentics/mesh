package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.SCHEMA_CONTAINER_KEY_PROPERTY;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static com.gentics.mesh.handler.VersionHandler.CURRENT_API_BASE_PATH;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.root.impl.SchemaRootImpl;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaUpdateModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.tinkerpop.blueprints.Vertex;

/**
 * @see Schema
 */
public class SchemaContainerImpl extends
		AbstractGraphFieldSchemaContainer<SchemaResponse, SchemaUpdateModel, SchemaReference, Schema, SchemaVersion> implements
	Schema {

	@Override
	protected Class<? extends Schema> getContainerClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	protected Class<? extends SchemaVersion> getContainerVersionClass() {
		return SchemaVersionImpl.class;
	}

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(SchemaContainerImpl.class, MeshVertexImpl.class);
	}

	@Override
	public SchemaReference transformToReference() {
		return new SchemaReferenceImpl().setName(getName()).setUuid(getUuid());
	}

	@Override
	public TraversalResult<? extends SchemaRoot> getRoots() {
		return in(HAS_SCHEMA_CONTAINER_ITEM, SchemaRootImpl.class);
	}

	@Override
	public RootVertex<Schema> getRoot() {
		return mesh().boot().meshRoot().getSchemaContainerRoot();
	}

	@Override
	public TraversalResult<? extends NodeImpl> getNodes() {
		Iterator<Vertex> vertices = mesh().database().getVertices(
			NodeImpl.class,
			new String[]{SCHEMA_CONTAINER_KEY_PROPERTY},
			new Object[]{getUuid()}
		);
		return new TraversalResult<>(graph.frameExplicit(vertices, NodeImpl.class));
	}

	@Override
	public void delete(BulkActionContext bac) {
		// Check whether the schema is currently being referenced by nodes.
		Iterator<? extends NodeImpl> it = getNodes().iterator();
		if (!it.hasNext()) {

			unassignEvents().forEach(bac::add);
			bac.add(onDeleted());

			for(SchemaVersion v : findAll()) {
				v.delete(bac);
			}
			remove();
		} else {
			throw error(BAD_REQUEST, "schema_delete_still_in_use", getUuid());
		}
	}

	/**
	 * Returns events for unassignment on deletion.
	 * @return
	 */
	private Stream<ProjectSchemaEventModel> unassignEvents() {
		return getRoots().stream()
			.map(SchemaRoot::getProject)
			.filter(Objects::nonNull)
			.map(project -> project.onSchemaAssignEvent(this, UNASSIGNED));
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return CURRENT_API_BASE_PATH + "/schemas/" + getUuid();
	}

	@Override
	public User getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public User getEditor() {
		return mesh().userProperties().getEditor(this);
	}

}
