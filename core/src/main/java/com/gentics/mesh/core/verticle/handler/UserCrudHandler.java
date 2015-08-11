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
import static org.apache.commons.lang3.StringUtils.isEmpty;
import io.vertx.ext.web.RoutingContext;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.util.BlueprintTransaction;

@Component
public class UserCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleDelete(RoutingContext rc) {
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			delete(rc, "uuid", "user_deleted", boot.userRoot());
			tx.success();
		}
	}

	@Override
	public void handleCreate(RoutingContext rc) {
		UserCreateRequest requestModel = fromJson(rc, UserCreateRequest.class);
		if (requestModel == null) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_parse_request_json_error")));
			return;
		}
		if (isEmpty(requestModel.getPassword())) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "user_missing_password")));
			return;
		}
		if (isEmpty(requestModel.getUsername())) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "user_missing_username")));
			return;
		}
		String groupUuid = requestModel.getGroupUuid();
		if (isEmpty(groupUuid)) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "user_missing_parentgroup_field")));
			return;
		}

		// Load the parent group for the user
		loadObjectByUuid(rc, groupUuid, CREATE_PERM, boot.groupRoot(), rh -> {
			if (hasSucceeded(rc, rh)) {
				Group parentGroup = rh.result();
				if (boot.userRoot().findByUsername(requestModel.getUsername()) != null) {
					String message = i18n.get(rc, "user_conflicting_username");
					rc.fail(new HttpStatusCodeErrorException(CONFLICT, message));
				} else {
					try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
						MeshAuthUser requestUser = getUser(rc);
						User user = boot.userRoot().create(requestModel.getUsername(), parentGroup, requestUser);
						//							User user = parentGroup.createUser(requestModel.getUsername());
						user.fillCreateFromRest(rc, requestModel, parentGroup, ch -> {
							if (ch.failed()) {
								rc.fail(ch.cause());
							} else {
								User createdUser = ch.result();
								searchQueue.put(user.getUuid(), User.TYPE, SearchQueueEntryAction.CREATE_ACTION);
								vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
								transformAndResponde(rc, createdUser);
							}
						});
					}
				}
			}
		});
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		loadObject(rc, "uuid", UPDATE_PERM, boot.userRoot(), rh -> {
			if (hasSucceeded(rc, rh)) {
				User user = rh.result();
				UserUpdateRequest requestModel = fromJson(rc, UserUpdateRequest.class);

				//TODO not sure whether this is actually correct. The try-with might terminate while the async call may still run 
				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					user.fillUpdateFromRest(rc, requestModel, uh -> {
						if (uh.failed()) {
							rc.fail(uh.cause());
							tx.failure();
						} else {
							searchQueue.put(user.getUuid(), User.TYPE, SearchQueueEntryAction.UPDATE_ACTION);
							vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
							tx.success();
							transformAndResponde(rc, uh.result());
						}
					});
				}
			}
		});
	}

	@Override
	public void handleRead(RoutingContext rc) {
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			loadObject(rc, "uuid", READ_PERM, boot.userRoot(), rh -> {
				loadTransformAndResponde(rc, "uuid", READ_PERM, boot.userRoot());
			});
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			loadTransformAndResponde(rc, boot.userRoot(), new UserListResponse());
		}
	}

}
