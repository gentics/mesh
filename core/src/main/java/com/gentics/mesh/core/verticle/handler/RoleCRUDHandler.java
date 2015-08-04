package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuid;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.util.BlueprintTransaction;

@Component
public class RoleCRUDHandler extends AbstractCRUDHandler {

	@Override
	public void handleCreate(RoutingContext rc) {
		RoleCreateRequest requestModel = fromJson(rc, RoleCreateRequest.class);
		MeshAuthUser requestUser = getUser(rc);
		if (StringUtils.isEmpty(requestModel.getName())) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_name_must_be_set")));
			return;
		}

		if (StringUtils.isEmpty(requestModel.getGroupUuid())) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "role_missing_parentgroup_field")));
			return;
		}

		if (boot.roleRoot().findByName(requestModel.getName()) != null) {
			rc.fail(new HttpStatusCodeErrorException(CONFLICT, i18n.get(rc, "role_conflicting_name")));
			return;
		}
		Future<Role> roleCreated = Future.future();
		loadObjectByUuid(rc, requestModel.getGroupUuid(), CREATE_PERM, boot.groupRoot(), rh -> {
			if (hasSucceeded(rc, rh)) {
				Group parentGroup = rh.result();
				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					Role role = boot.roleRoot().create(requestModel.getName(), parentGroup, requestUser);
					requestUser.addCRUDPermissionOnRole(parentGroup, CREATE_PERM, role);
					tx.success();
					roleCreated.complete(role);
				}
				Role role = roleCreated.result();
				searchQueue.put(role.getUuid(), Role.TYPE, SearchQueueEntryAction.CREATE_ACTION);
				vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
				transformAndResponde(rc, role);
			}
		});
	}

	@Override
	public void handleDelete(RoutingContext rc) {
		delete(rc, "uuid", "role_deleted", boot.roleRoot());
	}

	@Override
	public void handleRead(RoutingContext rc) {
		loadTransformAndResponde(rc, "uuid", READ_PERM, boot.roleRoot());
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		loadObject(rc, "uuid", UPDATE_PERM, boot.roleRoot(), rh -> {
			if (hasSucceeded(rc, rh)) {
				Role role = rh.result();
				RoleUpdateRequest requestModel = fromJson(rc, RoleUpdateRequest.class);

				if (!StringUtils.isEmpty(requestModel.getName()) && role.getName() != requestModel.getName()) {
					if (boot.roleRoot().findByName(requestModel.getName()) != null) {
						rc.fail(new HttpStatusCodeErrorException(CONFLICT, i18n.get(rc, "role_conflicting_name")));
						return;
					}
					role.setName(requestModel.getName());
				}
				searchQueue.put(role.getUuid(), Role.TYPE, SearchQueueEntryAction.UPDATE_ACTION);
				vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
				transformAndResponde(rc, role);
			}
		});
	}
	
	@Override
	public void handleReadList(RoutingContext rc) {
		loadTransformAndResponde(rc, boot.roleRoot(), new RoleListResponse());
	}
}
