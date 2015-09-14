package com.gentics.mesh.core.verticle.group;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
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
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.graphdb.NonTrx;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.InvalidArgumentException;

@Component
public class GroupCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			createObject(ac, boot.groupRoot());
		}
	}

	@Override
	public void handleDelete(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			deleteObject(ac, "uuid", "group_deleted", boot.groupRoot());
		}
	}

	@Override
	public void handleUpdate(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			updateObject(ac, "uuid", boot.groupRoot());
		}
	}

	@Override
	public void handleRead(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			loadTransformAndResponde(ac, "uuid", READ_PERM, boot.groupRoot());
		}
	}

	@Override
	public void handleReadList(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			loadTransformAndResponde(ac, boot.groupRoot(), new GroupListResponse());
		}
	}

	public void handleGroupRolesList(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			PagingInfo pagingInfo = ac.getPagingInfo();
			MeshAuthUser requestUser = ac.getUser();
			loadObject(ac, "groupUuid", READ_PERM, boot.groupRoot(), grh -> {
				try {
					Page<? extends Role> rolePage = grh.result().getRoles(requestUser, pagingInfo);
					transformAndResponde(ac, rolePage, new RoleListResponse());
				} catch (InvalidArgumentException e) {
					ac.fail(e);
				}
			});
		}
	}

	public void handleAddRoleToGroup(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			loadObject(ac, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(ac, grh)) {
					loadObject(ac, "roleUuid", READ_PERM, boot.roleRoot(), rrh -> {
						if (hasSucceeded(ac, rrh)) {
							Group group = grh.result();
							Role role = rrh.result();
							try (Trx txAdd = db.trx()) {
								group.addRole(role);
								txAdd.success();
							}
							transformAndResponde(ac, group);
						}
					});
				}
			});
		}
	}

	public void handleRemoveRoleFromGroup(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			loadObject(ac, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(ac, grh)) {
					// TODO check whether the role is actually part of the group
					loadObject(ac, "roleUuid", READ_PERM, boot.roleRoot(), rrh -> {
						if (hasSucceeded(ac, rrh)) {
							Group group = grh.result();
							Role role = rrh.result();
							try (Trx txRemove = db.trx()) {
								group.removeRole(role);
								txRemove.success();
							}
							transformAndResponde(ac, group);
						}
					});
				}
			});
		}
	}

	public void handleGroupUserList(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			MeshAuthUser requestUser = ac.getUser();
			PagingInfo pagingInfo = ac.getPagingInfo();
			loadObject(ac, "groupUuid", READ_PERM, boot.groupRoot(), grh -> {

				if (hasSucceeded(ac, grh)) {
					Group group = grh.result();
					Page<? extends User> userPage;
					try {
						userPage = group.getVisibleUsers(requestUser, pagingInfo);
						transformAndResponde(ac, userPage, new UserListResponse());
					} catch (Exception e) {
						ac.fail(e);
					}
				}
			});
		}
	}

	public void handleAddUserToGroup(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			loadObject(ac, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(ac, grh)) {
					loadObject(ac, "userUuid", READ_PERM, boot.userRoot(), urh -> {
						if (hasSucceeded(ac, urh)) {
							try (Trx txAdd = db.trx()) {
								Group group = grh.result();
								User user = urh.result();
								group.addUser(user);
								txAdd.success();
							}
							Group group = grh.result();
							transformAndResponde(ac, group);
						}
					});
				}
			});
		}
	}

	public void handleRemoveUserFromGroup(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			loadObject(ac, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(ac, grh)) {
					loadObject(ac, "userUuid", READ_PERM, boot.userRoot(), urh -> {
						if (hasSucceeded(ac, urh)) {
							try (Trx txRemove = db.trx()) {
								Group group = grh.result();
								User user = urh.result();
								group.removeUser(user);
								txRemove.success();
							}
							Group group = grh.result();
							transformAndResponde(ac, group);
						}
					});
				}
			});
		}
	}

}
