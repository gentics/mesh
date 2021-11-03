package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.Bucket.BUCKET_ID_KEY;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.BRANCH_UUID_KEY;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.EDGE_TYPE_KEY;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FROM_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TO_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.SCHEMA_CONTAINER_VERSION_KEY_PROPERTY;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.stream.Stream;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.etc.config.ContentConfig;
import com.tinkerpop.blueprints.Direction;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see SchemaVersion
 */
public class SchemaContainerVersionImpl extends
	AbstractGraphFieldSchemaContainerVersion<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchemaVersion, HibSchema> implements
	SchemaVersion {

	private static final Logger log = LoggerFactory.getLogger(SchemaContainerVersionImpl.class);

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(SchemaContainerVersionImpl.class, MeshVertexImpl.class);
	}

	@Override
	public Class<? extends HibSchemaVersion> getContainerVersionClass() {
		return SchemaContainerVersionImpl.class;
	}

	@Override
	public Class<? extends HibSchema> getContainerClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	public Result<? extends NodeGraphFieldContainer> getDraftFieldContainers(String branchUuid) {
		return new TraversalResult<>(toStream(mesh().database().getVertices(
			NodeGraphFieldContainerImpl.class,
			new String[] { SCHEMA_CONTAINER_VERSION_KEY_PROPERTY },
			new Object[] { getUuid() })).filter(
				v -> toStream(v.getEdges(Direction.IN, HAS_FIELD_CONTAINER))
					.anyMatch(
						e -> e.getProperty(BRANCH_UUID_KEY).equals(branchUuid) && ContainerType.get(e.getProperty(EDGE_TYPE_KEY)).equals(DRAFT)))
				.map(v -> graph.frameElementExplicit(v, NodeGraphFieldContainerImpl.class)));
	}

	@Override
	public Result<? extends HibNode> getNodes(String branchUuid, HibUser user, ContainerType type) {
		UserDao userDao = Tx.get().userDao();
		SchemaDao schemaDao = Tx.get().schemaDao();
		return new TraversalResult<>(schemaDao.getNodes(getSchemaContainer()).stream()
			.filter(node -> GraphFieldContainerEdgeImpl.matchesBranchAndType(node.getId(), branchUuid, type)
				&& userDao.hasPermissionForId(user, node.getId(), READ_PUBLISHED_PERM)));
	}

	@Override
	public Stream<NodeGraphFieldContainerImpl> getFieldContainers(String branchUuid) {
		return toStream(mesh().database().getVertices(
			NodeGraphFieldContainerImpl.class,
			new String[] { SCHEMA_CONTAINER_VERSION_KEY_PROPERTY },
			new Object[] { getUuid() })).filter(
				v -> toStream(v.getEdges(Direction.IN, HAS_FIELD_CONTAINER))
					.anyMatch(e -> e.getProperty(BRANCH_UUID_KEY).equals(branchUuid)))
				.map(v -> graph.frameElementExplicit(v, NodeGraphFieldContainerImpl.class));
	}


	@Override
	public Stream<NodeGraphFieldContainerImpl> getFieldContainers(String branchUuid, Bucket bucket) {
		return toStream(mesh().database().getVerticesForRange(
			NodeGraphFieldContainerImpl.class,
			"bucket",
			new String[] { SCHEMA_CONTAINER_VERSION_KEY_PROPERTY },
			new Object[] { getUuid() }, BUCKET_ID_KEY, (long) bucket.start(), (long) bucket.end())).filter(
				v -> toStream(v.getEdges(Direction.IN, HAS_FIELD_CONTAINER))
					.anyMatch(e -> e.getProperty(BRANCH_UUID_KEY).equals(branchUuid)))
				.map(v -> graph.frameElementExplicit(v, NodeGraphFieldContainerImpl.class));
	}

	@Override
	public Result<? extends Branch> getBranches() {
		return in(HAS_SCHEMA_VERSION, BranchImpl.class);
	}

	@Override
	public Iterable<? extends HibJob> referencedJobsViaTo() {
		return in(HAS_TO_VERSION).frame(Job.class);
	}

	@Override
	public Result<HibJob> referencedJobsViaFrom() {
		return new TraversalResult<>(in(HAS_FROM_VERSION).frame(Job.class));
	}

	@Override
	public MeshElementEventModel onCreated() {
		return getSchemaContainer().onCreated();
	}

	@Override
	public MeshElementEventModel onUpdated() {
		return getSchemaContainer().onUpdated();
	}

	@Override
	public boolean isAutoPurgeEnabled() {
		Boolean schemaAutoPurge = getSchema().getAutoPurge();
		if (schemaAutoPurge == null) {
			if (log.isDebugEnabled()) {
				log.debug("No schema auto purge flag set. Falling back to mesh global setting");
			}
			ContentConfig contentOptions = options().getContentOptions();
			if (contentOptions != null) {
				return contentOptions.isAutoPurge();
			} else {
				return true;
			}
		} else {
			return schemaAutoPurge;
		}
	}
}
