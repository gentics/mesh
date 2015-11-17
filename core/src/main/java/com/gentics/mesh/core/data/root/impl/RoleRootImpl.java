package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.rest.error.HttpConflictErrorException.conflict;
import static com.gentics.mesh.util.VerticleHelper.processOrFail;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class RoleRootImpl extends AbstractRootVertex<Role>implements RoleRoot {

	private static final Logger log = LoggerFactory.getLogger(RoleRootImpl.class);

	public static void checkIndices(Database database) {
		database.addEdgeIndex(HAS_ROLE);
		database.addVertexType(RoleRootImpl.class);
	}

	@Override
	protected Class<? extends Role> getPersistanceClass() {
		return RoleImpl.class;
	}

	@Override
	protected String getRootLabel() {
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

	public void create(InternalActionContext ac, Handler<AsyncResult<Role>> handler) {
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		Database db = MeshSpringConfiguration.getInstance().database();

		RoleCreateRequest requestModel = ac.fromJson(RoleCreateRequest.class);
		String roleName = requestModel.getName();

		MeshAuthUser requestUser = ac.getUser();
		if (StringUtils.isEmpty(roleName)) {
			handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("error_name_must_be_set"))));
			return;
		}

		Role conflictingRole = findByName(roleName);
		if (conflictingRole != null) {
			HttpStatusCodeErrorException conflictError = conflict(ac, conflictingRole.getUuid(), roleName, "role_conflicting_name");
			handler.handle(Future.failedFuture(conflictError));
			return;
		}

		// TODO use non-blocking code here
		if (requestUser.hasPermission(this, CREATE_PERM)) {
			db.trx(txCreate -> {
				requestUser.reload();
				Role role = create(requestModel.getName(), requestUser);
				requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, role);
				SearchQueueBatch batch = role.addIndexBatch(SearchQueueEntryAction.CREATE_ACTION);
				txCreate.complete(Tuple.tuple(batch, role));
			} , (AsyncResult<Tuple<SearchQueueBatch, Role>> txCreated) -> {
				if (txCreated.failed()) {
					handler.handle(Future.failedFuture(txCreated.cause()));
				} else {
					processOrFail(ac, txCreated.result().v1(), handler, txCreated.result().v2());
				}
			});
		} else {
			handler.handle(Future.failedFuture(new InvalidPermissionException(ac.i18n("error_missing_perm", this.getUuid()))));
		}

	}

	@Override
	public void delete() {
		throw new NotImplementedException("The role root node can't be deleted");
	}

}
