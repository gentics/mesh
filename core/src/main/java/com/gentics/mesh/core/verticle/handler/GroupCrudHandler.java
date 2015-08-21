package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.getPagingInfo;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.updateObject;

import org.springframework.stereotype.Component;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.ext.web.RoutingContext;

@Component
public class GroupCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			createObject(rc, boot.groupRoot());
		}
	}

	@Override
	public void handleDelete(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			deleteObject(rc, "uuid", "group_deleted", boot.groupRoot());
		}
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			updateObject(rc, "uuid", boot.groupRoot());
		}

	}

	@Override
	public void handleRead(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadTransformAndResponde(rc, "uuid", READ_PERM, boot.groupRoot());
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadTransformAndResponde(rc, boot.groupRoot(), new GroupListResponse());
		}
	}

	public void handleGroupRolesList(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
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
	}

	public void handleAddRoleToGroup(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadObject(rc, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(rc, grh)) {
					loadObject(rc, "roleUuid", READ_PERM, boot.roleRoot(), rrh -> {
						if (hasSucceeded(rc, rrh)) {
							Group group = grh.result();
							Role role = rrh.result();
							try (Trx txAdd = new Trx(db)) {
								group.addRole(role);
								txAdd.success();
							}
							transformAndResponde(rc, group);
						}
					});
				}
			});
		}
	}

	public void handleRemoveRoleFromGroup(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadObject(rc, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(rc, grh)) {
					// TODO check whether the role is actually part of the group
					loadObject(rc, "roleUuid", READ_PERM, boot.roleRoot(), rrh -> {
						if (hasSucceeded(rc, rrh)) {
							Group group = grh.result();
							Role role = rrh.result();
							try (Trx txRemove = new Trx(db)) {
								group.removeRole(role);
								txRemove.success();
							}
							transformAndResponde(rc, group);
						}
					});
				}
			});
		}
	}

	public void handleGroupUserList(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
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
	}

	public void handleAddUserToGroup(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadObject(rc, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(rc, grh)) {
					loadObject(rc, "userUuid", READ_PERM, boot.userRoot(), urh -> {
						if (hasSucceeded(rc, urh)) {
							try (Trx txAdd = new Trx(db)) {
								Group group = grh.result();
								User user = urh.result();
								group.addUser(user);
								txAdd.success();
							}
							Group group = grh.result();
							transformAndResponde(rc, group);
						}
					});
				}
			});
		}
	}

	public void handleRemoveUserFromGroup(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadObject(rc, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(rc, grh)) {
					loadObject(rc, "userUuid", READ_PERM, boot.userRoot(), urh -> {
						if (hasSucceeded(rc, urh)) {
							try (Trx txRemove = new Trx(db)) {
								Group group = grh.result();
								User user = urh.result();
								group.removeUser(user);
								txRemove.success();
							}
							Group group = grh.result();
							transformAndResponde(rc, group);
						}
					});
				}
			});
		}
	}

}
