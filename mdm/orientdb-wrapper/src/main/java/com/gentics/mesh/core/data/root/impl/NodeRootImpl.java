package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.PROJECT_KEY_PROPERTY;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.Set;
import java.util.stream.Stream;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformableStreamPageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see NodeRoot
 */
public class NodeRootImpl extends AbstractRootVertex<Node> implements NodeRoot {

	private static final Logger log = LoggerFactory.getLogger(NodeRootImpl.class);

	/**
	 * Initialize the graph vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(NodeRootImpl.class, MeshVertexImpl.class);
		index.createIndex(edgeIndex(HAS_NODE).withInOut().withOut());
	}

	@Override
	public Class<? extends Node> getPersistanceClass() {
		return NodeImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_NODE;
	}

	@Override
	public Page<? extends Node> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
		return new DynamicTransformableStreamPageImpl<>(findAllStream(ac, type), pagingInfo);
	}

	@Override
	public Result<? extends Node> findAll() {
		Project project = getProject();
		return project.findNodes();
	}

	private Project getProject() {
		return in(HAS_NODE_ROOT, ProjectImpl.class).next();
	}

	@Override
	public long globalCount() {
		return db().count(NodeImpl.class);
	}

	@Override
	public Stream<? extends Node> findAllStream(InternalActionContext ac, InternalPermission perm) {
		Tx tx = Tx.get();
		HibUser user = ac.getUser();
		String branchUuid = tx.getBranch(ac).getUuid();
		UserDao userDao = mesh().boot().userDao();

		return findAll(tx.getProject(ac).getUuid())
			.filter(item -> {
				boolean hasRead = userDao.hasPermissionForId(user, item.getId(), READ_PERM);
				if (hasRead) {
					return true;
				} else {
					// Check whether the node is published. In this case we need to check the read publish perm.
					boolean isPublishedForBranch = GraphFieldContainerEdgeImpl.matchesBranchAndType(item.getId(), branchUuid, PUBLISHED);
					if (isPublishedForBranch) {
						return userDao.hasPermissionForId(user, item.getId(), READ_PUBLISHED_PERM);
					}
				}
				return false;
			})
			.map(vertex -> graph.frameElementExplicit(vertex, getPersistanceClass()));
	}

	/**
	 * Finds all nodes of a project.
	 * 
	 * @param projectUuid
	 * @return
	 */
	private Stream<Vertex> findAll(String projectUuid) {
		return toStream(db().getVertices(
			NodeImpl.class,
			new String[] { PROJECT_KEY_PROPERTY },
			new Object[] { projectUuid }));
	}

	private Stream<? extends Node> findAllStream(InternalActionContext ac, ContainerType type) {
		HibUser user = ac.getUser();
		FramedTransactionalGraph graph = GraphDBTx.getGraphTx().getGraph();

		HibBranch branch = Tx.get().getBranch(ac);
		String branchUuid = branch.getUuid();
		UserDao userDao = mesh().boot().userDao();

		return findAll(Tx.get().getProject(ac).getUuid()).filter(item -> {
			// Check whether the node has at least one content of the type in the selected branch - Otherwise the node should be skipped
			return GraphFieldContainerEdgeImpl.matchesBranchAndType(item.getId(), branchUuid, type);
		}).filter(item -> {
			boolean hasRead = userDao.hasPermissionForId(user, item.getId(), READ_PERM);
			if (hasRead) {
				return true;
			} else if (type == PUBLISHED) {
				// Check whether the node is published. In this case we need to check the read publish perm.
				boolean isPublishedForBranch = GraphFieldContainerEdgeImpl.matchesBranchAndType(item.getId(), branchUuid, PUBLISHED);
				if (isPublishedForBranch) {
					return userDao.hasPermissionForId(user, item.getId(), READ_PUBLISHED_PERM);
				}
			}
			return false;
		})
			.map(vertex -> graph.frameElementExplicit(vertex, getPersistanceClass()));
	}

	@Override
	public Node findByUuid(String uuid) {
		return getProject().findNode(uuid);
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}

	@Override
	public boolean applyPermissions(EventQueueBatch batch, HibRole role, boolean recursive,
		Set<InternalPermission> permissionsToGrant, Set<InternalPermission> permissionsToRevoke) {
		boolean permissionChanged = false;
		if (recursive) {
			for (Node node : findAll()) {
				// We don't need to recursively handle the permissions for each node again since
				// this call will already affect all nodes.
				permissionChanged = node.applyPermissions(batch, role, false, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			}
		}

		permissionChanged = super.applyPermissions(batch, toGraph(role), false, permissionsToGrant, permissionsToRevoke) || permissionChanged;
		return permissionChanged;
	}
}
