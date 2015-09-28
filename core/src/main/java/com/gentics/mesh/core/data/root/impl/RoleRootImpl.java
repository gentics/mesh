package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuid;
import static com.gentics.mesh.util.VerticleHelper.processOrFail;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
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
	public Role create(String name, Group group, User creator) {
		Role role = getGraph().addFramedVertex(RoleImpl.class);
		role.setName(name);
		role.setCreator(creator);
		role.setCreationTimestamp(System.currentTimeMillis());
		role.setEditor(creator);
		role.setLastEditedTimestamp(System.currentTimeMillis());

		addRole(role);
		if (group != null) {
			group.addRole(role);
		}
		return role;
	}

	public void create(InternalActionContext ac, Handler<AsyncResult<Role>> handler) {
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		Database db = MeshSpringConfiguration.getInstance().database();

		RoleCreateRequest requestModel = ac.fromJson(RoleCreateRequest.class);
		MeshAuthUser requestUser = ac.getUser();
		if (StringUtils.isEmpty(requestModel.getName())) {
			handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("error_name_must_be_set"))));
			return;
		}

		// TODO disable this check. It should be possible to create a role without specifying the group uuid.
		if (StringUtils.isEmpty(requestModel.getGroupUuid())) {
			handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("role_missing_parentgroup_field"))));
			return;
		}

		if (findByName(requestModel.getName()) != null) {
			handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(CONFLICT, ac.i18n("role_conflicting_name"))));
			return;
		}

		// TODO use blocking code here
		loadObjectByUuid(ac, requestModel.getGroupUuid(), CREATE_PERM, boot.groupRoot(), rh -> {
			if (rh.succeeded()) {
				db.blockingTrx(txCreate -> {
					Group parentGroup = rh.result();
					requestUser.reload();
					Role role = create(requestModel.getName(), parentGroup, requestUser);
					requestUser.addCRUDPermissionOnRole(parentGroup, CREATE_PERM, role);
					SearchQueueBatch batch = role.addIndexBatch(SearchQueueEntryAction.CREATE_ACTION);
					txCreate.complete(Tuple.tuple(batch, role));
				} , (AsyncResult<Tuple<SearchQueueBatch, Role>> txCreated) -> {
					if (txCreated.failed()) {
						handler.handle(Future.failedFuture(txCreated.cause()));
					} else {
						processOrFail(ac, txCreated.result().v1(), handler, txCreated.result().v2());
					}
				});
				return;
			} else {
				if (rh.cause() != null) {
					handler.handle(Future.failedFuture(rh.cause()));
				} else {
					handler.handle(Future.failedFuture(
							new HttpStatusCodeErrorException(BAD_REQUEST, "Could not load group {" + requestModel.getGroupUuid() + "}", rh.cause())));
				}
			}
		});
	}

	@Override
	public void delete() {
		throw new NotImplementedException("The role root node can't be deleted");
	}

}
