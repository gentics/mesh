package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.VerticleHelper.getPagingInfo;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.ext.web.RoutingContext;

@Component
public class GroupCrudHandler extends AbstractCrudHandler {

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
				try (Trx tx = new Trx(database)) {
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
		try (Trx tx = new Trx(database)) {
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
						rc.fail(new HttpStatusCodeErrorException(CONFLICT, i18n.get(rc, "group_conflicting_name")));
						return;
					}
					group.setName(requestModel.getName());
				}
				searchQueue().put(group.getUuid(), Group.TYPE, SearchQueueEntryAction.UPDATE_ACTION);
				vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
				transformAndResponde(rc, group);
			}
		});

	}

	@Override
	public void handleRead(RoutingContext rc) {
		try (Trx tx = new Trx(database)) {
			loadTransformAndResponde(rc, "uuid", READ_PERM, boot.groupRoot());
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		try (Trx tx = new Trx(database)) {
			loadTransformAndResponde(rc, boot.groupRoot(), new GroupListResponse());
		}
	}

	public void handleGroupRolesList(RoutingContext rc) {
		PagingInfo pagingInfo = getPagingInfo(rc);
		MeshAuthUser requestUser = getUser(rc);

		loadObject(rc, "groupUuid", READ_PERM, boot.groupRoot(), grh -> {
			try {
				Page<? extends Role> rolePage = grh.result().getRoles(requestUser, pagingInfo);
				transformAndResponde(rc, rolePage, new RoleListResponse());
			} catch (InvalidArgumentException e) {
				rc.fail(e);
			}
		});
	}

	public void handleAddRoleToGroup(RoutingContext rc) {
		loadObject(rc, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
			if (hasSucceeded(rc, grh)) {
				loadObject(rc, "roleUuid", READ_PERM, boot.roleRoot(), rrh -> {
					if (hasSucceeded(rc, rrh)) {
						try (Trx tx = new Trx(database)) {
							Group group = grh.result();
							Role role = rrh.result();
							group.addRole(role);
							tx.success();
							transformAndResponde(rc, group);
						}
					}
				});
			}
		});
	}

	public void handleRemoveRoleFromGroup(RoutingContext rc) {
		loadObject(rc, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
			if (hasSucceeded(rc, grh)) {
				// TODO check whether the role is actually part of the group
				loadObject(rc, "roleUuid", READ_PERM, boot.roleRoot(), rrh -> {
					if (hasSucceeded(rc, rrh)) {
						try (Trx tx = new Trx(database)) {
							Group group = grh.result();
							Role role = rrh.result();
							group.removeRole(role);
							tx.success();
							transformAndResponde(rc, group);
						}
					}
				});
			}
		});
	}

	public void handleGroupUserList(RoutingContext rc) {
		MeshAuthUser requestUser = getUser(rc);

		PagingInfo pagingInfo = getPagingInfo(rc);

		loadObject(rc, "groupUuid", READ_PERM, boot.groupRoot(), grh -> {

			if (hasSucceeded(rc, grh)) {
				Group group = grh.result();
				Page<? extends User> userPage;
				try {
					userPage = group.getVisibleUsers(requestUser, pagingInfo);
					transformAndResponde(rc, userPage, new UserListResponse());
				} catch (Exception e) {
					rc.fail(e);
				}
			}
		});
	}

	public void handleAddUserToGroup(RoutingContext rc) {
		loadObject(rc, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
			if (hasSucceeded(rc, grh)) {
				loadObject(rc, "userUuid", READ_PERM, boot.userRoot(), urh -> {
					if (hasSucceeded(rc, urh)) {
						try (Trx tx = new Trx(database)) {
							Group group = grh.result();
							User user = urh.result();
							group.addUser(user);
							tx.success();
						}
						Group group = grh.result();
						transformAndResponde(rc, group);
					}
				});
			}
		});
	}

	public void handleRemoveUserFromGroup(RoutingContext rc) {
		loadObject(rc, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
			if (hasSucceeded(rc, grh)) {
				loadObject(rc, "userUuid", READ_PERM, boot.userRoot(), urh -> {
					if (hasSucceeded(rc, urh)) {
						try (Trx tx = new Trx(database)) {
							Group group = grh.result();
							User user = urh.result();
							group.removeUser(user);
							tx.success();
						}
						Group group = grh.result();
						transformAndResponde(rc, group);
					}
				});
			}
		});
	}

}
