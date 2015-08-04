package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import io.vertx.ext.web.RoutingContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.BlueprintTransaction;
@Component
public class GroupCRUDHandler extends AbstractCRUDHandler {

	@Override
	public void handleCreate(RoutingContext rc) {
		MeshAuthUser requestUser = getUser(rc);
		GroupCreateRequest requestModel = JsonUtil.fromJson(rc, GroupCreateRequest.class);

		if (StringUtils.isEmpty(requestModel.getName())) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_name_must_be_set")));
			return;
		}

		MeshRoot root = boot.meshRoot();
		GroupRoot groupRoot = root.getGroupRoot();
		if (requestUser.hasPermission(groupRoot, CREATE_PERM)) {
			if (groupRoot.findByName(requestModel.getName()) != null) {
				rc.fail(new HttpStatusCodeErrorException(CONFLICT, i18n.get(rc, "group_conflicting_name")));
			} else {
				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					Group group = groupRoot.create(requestModel.getName(), requestUser);
					requestUser.addCRUDPermissionOnRole(root.getGroupRoot(), CREATE_PERM, group);
					tx.success();
					transformAndResponde(rc, group);
				}
			}
		} else {
			rc.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", groupRoot.getUuid())));
		}
	}

	@Override
	public void handleDelete(RoutingContext rc) {
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			delete(rc, "uuid", "group_deleted", boot.groupRoot());
		}
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		loadObject(rc, "uuid", UPDATE_PERM, boot.groupRoot(), grh -> {
			if (hasSucceeded(rc, grh)) {
				Group group = grh.result();
				GroupUpdateRequest requestModel = fromJson(rc, GroupUpdateRequest.class);

				if (StringUtils.isEmpty(requestModel.getName())) {
					rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_name_must_be_set")));
					return;
				}

				if (!group.getName().equals(requestModel.getName())) {
					Group groupWithSameName = boot.groupRoot().findByName(requestModel.getName());
					if (groupWithSameName != null && !groupWithSameName.getUuid().equals(group.getUuid())) {
						rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "group_conflicting_name")));
						return;
					}
					group.setName(requestModel.getName());
				}
				searchQueue.put(group.getUuid(), Group.TYPE, SearchQueueEntryAction.UPDATE_ACTION);
				vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
				transformAndResponde(rc, group);
			}
		});

	}

	@Override
	public void handleRead(RoutingContext rc) {
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			loadTransformAndResponde(rc, "uuid", READ_PERM, boot.groupRoot());
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			loadTransformAndResponde(rc, boot.groupRoot(), new GroupListResponse());
		}
	}

}
