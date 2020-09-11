package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import org.apache.commons.lang3.StringUtils;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.event.EventQueueBatch;

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
	public Role create(String name, User creator, String uuid) {
		Role role = getGraph().addFramedVertex(RoleImpl.class);
		if (uuid != null) {
			role.setUuid(uuid);
		}
		role.setName(name);
		role.setCreated(creator);
		role.generateBucketId();
		addRole(role);
		return role;
	}

	public Role create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		RoleCreateRequest requestModel = ac.fromJson(RoleCreateRequest.class);
		String roleName = requestModel.getName();

		MeshAuthUser requestUser = ac.getUser();
		if (StringUtils.isEmpty(roleName)) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}

		Role conflictingRole = findByName(roleName);
		if (conflictingRole != null) {
			throw conflict(conflictingRole.getUuid(), roleName, "role_conflicting_name");
		}

		// TODO use non-blocking code here
		if (!requestUser.hasPermission(this, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", this.getUuid(), CREATE_PERM.getRestPerm().getName());
		}

		Role role = create(requestModel.getName(), requestUser, uuid);
		requestUser.inheritRolePermissions(this, role);
		batch.add(role.onCreated());
		return role;

	}

	@Override
	public void delete(BulkActionContext bac) {
		throw error(INTERNAL_SERVER_ERROR, "The global role root can't be deleted.");
	}

}
