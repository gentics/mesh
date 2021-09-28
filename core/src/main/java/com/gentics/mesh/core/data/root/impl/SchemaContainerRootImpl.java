package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.SCHEMA_CONTAINER_KEY_PROPERTY;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.Iterator;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see SchemaRoot
 */
public class SchemaContainerRootImpl extends AbstractRootVertex<Schema> implements SchemaRoot {

	private static final Logger log = LoggerFactory.getLogger(SchemaContainerRootImpl.class);

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(SchemaContainerRootImpl.class, MeshVertexImpl.class);
		type.createType(edgeType(HAS_SCHEMA_ROOT));
		type.createType(edgeType(HAS_SCHEMA_CONTAINER_ITEM));
		index.createIndex(edgeIndex(HAS_SCHEMA_CONTAINER_ITEM).withInOut().withOut());
	}

	@Override
	public Class<? extends Schema> getPersistanceClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_SCHEMA_CONTAINER_ITEM;
	}

	@Override
	public void addSchemaContainer(HibUser user, HibSchema schema, EventQueueBatch batch) {
		addItem(toGraph(schema));
	}

	@Override
	public void removeSchemaContainer(HibSchema schemaContainer, EventQueueBatch batch) {
		removeItem(toGraph(schemaContainer));
	}

	@Override
	public long globalCount() {
		return toGraph(db()).count(SchemaContainerImpl.class);
	}

	@Override
	public boolean contains(HibSchema schema) {
		if (findByUuid(schema.getUuid()) == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void delete(BulkActionContext bac) {
		if (mesh().boot().meshRoot().getSchemaContainerRoot() == this) {
			throw error(INTERNAL_SERVER_ERROR, "Deletion of the global schema root is not possible");
		}
		if (log.isDebugEnabled()) {
			log.debug("Deleting schema container root {" + getUuid() + "}");
		}
		getElement().remove();
		bac.inc();
	}

	@Override
	public Schema create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		throw new RuntimeException("Wrong invocation. Use dao instead");
	}

	@Override
	public Schema create() {
		return getGraph().addFramedVertex(SchemaContainerImpl.class);
	}

	@Override
	public SchemaVersion createVersion() {
		return getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
	}

	/**
	 * Get the project
	 *
	 * @return project
	 */
	@Override
	public Project getProject() {
		return in(HAS_SCHEMA_ROOT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public Result<? extends SchemaRoot> getRoots(Schema schema) {
		return schema.in(HAS_SCHEMA_CONTAINER_ITEM, SchemaContainerRootImpl.class);
	}

	@Override
	public Result<? extends Node> getNodes(Schema schema) {
		Iterator<Vertex> vertices = mesh().database().getVertices(
			NodeImpl.class,
			new String[] { SCHEMA_CONTAINER_KEY_PROPERTY },
			new Object[] { schema.getUuid() });
		return new TraversalResult<>(graph.frameExplicit(vertices, NodeImpl.class));
	}

	@Override
	public Iterable<? extends SchemaVersion> findAllVersions(Schema schema) {
		return schema.out(HAS_PARENT_CONTAINER).frameExplicit(SchemaContainerVersionImpl.class);
	}
}
