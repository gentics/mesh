package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

/**
 * @see RoleRoot
 */
public class RoleRootImpl extends AbstractRootVertex<Role> implements RoleRoot {

	private static final Logger log = LoggerFactory.getLogger(RoleRootImpl.class);

	public static void init(Database database) {
		database.addVertexType(RoleRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_ROLE, true, false, true);
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
			log.debug("Adding role {" + role.getUuid() + ":" + role.getName() + "#" + role.getImpl().getId() + "} to roleRoot {" + getId() + "}");
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
	public Role create(String name, User creator) {
		Role role = getGraph().addFramedVertex(RoleImpl.class);
		role.setName(name);
		role.setCreated(creator);
		addRole(role);
		return role;
	}

	public Single<Role> create(InternalActionContext ac) {
		Database db = MeshInternal.get().database();

		RoleCreateRequest requestModel = ac.fromJson(RoleCreateRequest.class);
		String roleName = requestModel.getName();

		MeshAuthUser requestUser = ac.getUser();
		if (StringUtils.isEmpty(roleName)) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}

		Role conflictingRole = findByName(roleName).toBlocking().value();
		if (conflictingRole != null) {
			throw conflict(conflictingRole.getUuid(), roleName, "role_conflicting_name");
		}

		// TODO use non-blocking code here
		if (!requestUser.hasPermission(this, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", this.getUuid());
		}

		Tuple<SearchQueueBatch, Role> tuple = db.tx(() -> {
			requestUser.reload();
			Role role = create(requestModel.getName(), requestUser);
			requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, role);
			SearchQueueBatch batch = role.createIndexBatch(STORE_ACTION);
			return Tuple.tuple(batch, role);
		});

		SearchQueueBatch batch = tuple.v1();
		Role createdRole = tuple.v2();

		return batch.process().toSingleDefault(createdRole);

	}

	@Override
	public void delete(SearchQueueBatch batch) {
		throw new NotImplementedException("The role root node can't be deleted");
	}

}
