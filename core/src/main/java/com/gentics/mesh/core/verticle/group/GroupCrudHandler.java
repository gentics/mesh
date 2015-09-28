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
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

@Component
public class GroupCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			createObject(ac, boot.groupRoot());
		} , ac.errorHandler());
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			deleteObject(ac, "uuid", "group_deleted", boot.groupRoot());
		} , ac.errorHandler());
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			updateObject(ac, "uuid", boot.groupRoot());
		} , ac.errorHandler());
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			loadTransformAndResponde(ac, "uuid", READ_PERM, boot.groupRoot());
		} , ac.errorHandler());
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			loadTransformAndResponde(ac, boot.groupRoot(), new GroupListResponse());
		} , ac.errorHandler());
	}

	public void handleGroupRolesList(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
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
		} , ac.errorHandler());
	}

	public void handleAddRoleToGroup(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			loadObject(ac, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(ac, grh)) {
					loadObject(ac, "roleUuid", READ_PERM, boot.roleRoot(), rrh -> {
						if (hasSucceeded(ac, rrh)) {
							Group group = grh.result();
							Role role = rrh.result();
							db.blockingTrx(txAdd -> {
								group.addRole(role);
								txAdd.complete(group);
							} , (AsyncResult<Group> txAdded) -> {
								if (txAdded.failed()) {
									ac.errorHandler().handle(Future.failedFuture(txAdded.cause()));
								} else {
									transformAndResponde(ac, txAdded.result());
								}
							});
						}
					});
				}
			});
		} , ac.errorHandler());
	}

	public void handleRemoveRoleFromGroup(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			loadObject(ac, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(ac, grh)) {
					// TODO check whether the role is actually part of the group
					loadObject(ac, "roleUuid", READ_PERM, boot.roleRoot(), rrh -> {
						if (hasSucceeded(ac, rrh)) {
							Group group = grh.result();
							Role role = rrh.result();
							db.blockingTrx(txRemove -> {
								group.removeRole(role);
								txRemove.complete(group);
							} , (AsyncResult<Group> txAdded) -> {
								if (txAdded.failed()) {
									ac.errorHandler().handle(Future.failedFuture(txAdded.cause()));
								} else {
									transformAndResponde(ac, group);
								}
							});
						}
					});
				}
			});
		} , ac.errorHandler());
	}

	public void handleGroupUserList(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			MeshAuthUser requestUser = ac.getUser();
			PagingInfo pagingInfo = ac.getPagingInfo();
			loadObject(ac, "groupUuid", READ_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(ac, grh)) {
					try {
						Group group = grh.result();
						Page<? extends User> userPage = group.getVisibleUsers(requestUser, pagingInfo);
						transformAndResponde(ac, userPage, new UserListResponse());
					} catch (Exception e) {
						ac.fail(e);
					}
				}
			});
		} , ac.errorHandler());
	}

	public void handleAddUserToGroup(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {

			loadObject(ac, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(ac, grh)) {
					loadObject(ac, "userUuid", READ_PERM, boot.userRoot(), urh -> {
						if (hasSucceeded(ac, urh)) {
							db.blockingTrx(tcAdd -> {
								Group group = grh.result();
								User user = urh.result();
								group.addUser(user);
								tcAdd.complete(group);
							} , (AsyncResult<Group> addHandler) -> {
								if (addHandler.failed()) {
									ac.fail(addHandler.cause());
								} else {
									transformAndResponde(ac, addHandler.result());
								}
							});
						}
					});
				}
			});
		} , ac.errorHandler());
	}

	public void handleRemoveUserFromGroup(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			loadObject(ac, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(ac, grh)) {
					loadObject(ac, "userUuid", READ_PERM, boot.userRoot(), urh -> {
						if (hasSucceeded(ac, urh)) {
							db.blockingTrx(tcRemove -> {
								Group group = grh.result();
								User user = urh.result();
								group.removeUser(user);
								tcRemove.complete(group);
							} , (AsyncResult<Group> rh) -> {
								if (rh.failed()) {
									ac.fail(rh.cause());
								} else {
									transformAndResponde(ac, rh.result());
								}
							});
						}
					});
				}
			});
		} , ac.errorHandler());
	}

}
