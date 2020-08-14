package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see RoleRoot
 */
public class RoleRootImpl extends AbstractRootVertex<Role> implements RoleRoot {

	private static final Logger log = LoggerFactory.getLogger(RoleRootImpl.class);

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(RoleRootImpl.class, MeshVertexImpl.class);
		index.createIndex(edgeIndex(HAS_ROLE).withInOut().withOut());
	}

	@Override
	public Class<? extends Role> getPersistanceClass() {
		return RoleImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_ROLE;
	}

	@Override
	public void addRole(Role role) {
		if (log.isDebugEnabled()) {
			log.debug("Adding role {" + role.getUuid() + ":" + role.getName() + "#" + role.id() + "} to roleRoot {" + id() + "}");
		}
		addItem(role);
	}

	@Override
	public void removeRole(Role role) {
		// TODO delete the role? unlink from all groups? how is ferma / blueprint handling this. Neo4j would explode when trying to remove a node that still has
		// connecting edges.
		removeItem(role);
	}

	@Override
	public void delete(BulkActionContext bac) {
		throw error(INTERNAL_SERVER_ERROR, "The global role root can't be deleted.");
	}

	@Override
	public Page<? extends Group> getGroups(Role role, HibUser user, PagingParameters pagingInfo) {
		VertexTraversal<?, ?, ?> traversal = role.out(HAS_ROLE);
		return new DynamicTransformablePageImpl<Group>(user, traversal, pagingInfo, READ_PERM, GroupImpl.class);
	}

	public Role create() {
		return getGraph().addFramedVertex(RoleImpl.class);
	}

	@Override
	public Role create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		throw new RuntimeException("Wrong invocation. Use Dao instead.");
	}

	@Override
	public RoleResponse transformToRestSync(Role element, InternalActionContext ac, int level, String... languageTags) {
		throw new RuntimeException("Wrong invocation. Use Dao instead.");
	}

}
