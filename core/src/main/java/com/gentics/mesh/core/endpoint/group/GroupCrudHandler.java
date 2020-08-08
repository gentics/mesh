package com.gentics.mesh.core.endpoint.group;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.verticle.handler.CreateAction;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.LoadAction;
import com.gentics.mesh.core.verticle.handler.LoadAllAction;
import com.gentics.mesh.core.verticle.handler.UpdateAction;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler for group specific request methods.
 */
public class GroupCrudHandler extends AbstractCrudHandler<Group, GroupResponse> {

	private static final Logger log = LoggerFactory.getLogger(GroupCrudHandler.class);

	private Lazy<BootstrapInitializer> boot;

	@Inject
	public GroupCrudHandler(Database db, Lazy<BootstrapInitializer> boot, HandlerUtilities utils, WriteLock writeLock) {
		super(db, utils, writeLock);
		this.boot = boot;
	}

	@Override
	public LoadAction<Group> loadAction() {
		return (tx, ac, uuid, perm, errorIfNotFound) -> {
			return tx.data().groupDao().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
		};
	}

	@Override
	public LoadAllAction<Group> loadAllAction() {
		return (tx, ac, pagingInfo) -> {
			return tx.data().groupDao().findAll(ac, pagingInfo);
		};
	}

	@Override
	public CreateAction<Group> createAction() {
		return (tx, ac, batch, uuid) -> {
			return tx.data().groupDao().create(ac, batch, uuid);
		};
	}

	@Override
	public UpdateAction<Group> updateAction() {
		return (tx, group, ac, batch) -> {
			return tx.data().groupDao().update(group, ac, batch);
		};
	}

	/**
	 * Handle a read roles of group request.
	 * 
	 * @param ac
	 * @param groupUuid
	 *            Group Uuid from which the roles should be loaded
	 */
	public void handleGroupRolesList(InternalActionContext ac, String groupUuid) {
		utils.syncTx(ac, tx -> {
			GroupRoot groupRoot = tx.data().groupDao();
			Group group = tx.data().groupDao().loadObjectByUuid(ac, groupUuid, READ_PERM);
			PagingParametersImpl pagingInfo = new PagingParametersImpl(ac);
			TransformablePage<? extends Role> rolePage = groupRoot.getRoles(group, ac.getUser(), pagingInfo);
			return rolePage.transformToRestSync(ac, 0);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Add the role with the given uuid to the group.
	 * 
	 * @param ac
	 * @param groupUuid
	 * @param roleUuid
	 */
	public void handleAddRoleToGroup(InternalActionContext ac, String groupUuid, String roleUuid) {
		validateParameter(groupUuid, "groupUuid");
		validateParameter(roleUuid, "roleUuid");

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				GroupRoot groupDao = tx.data().groupDao();
				RoleRoot roleDao = tx.data().roleDao();
				Group group = groupDao.loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
				Role role = roleDao.loadObjectByUuid(ac, roleUuid, READ_PERM);
				// Handle idempotency
				if (groupDao.hasRole(group, role)) {
					if (log.isDebugEnabled()) {
						log.debug("Role {" + role.getUuid() + "} is already assigned to group {" + group.getUuid() + "}.");
					}
				} else {
					utils.eventAction(batch -> {
						groupDao.addRole(group, role);
						group.setEditor(ac.getUser());
						group.setLastEditedTimestamp();
						batch.add(groupDao.createRoleAssignmentEvent(group, role, ASSIGNED));
					});
				}
				return group.transformToRestSync(ac, 0);
			}, model -> ac.send(model, OK));
		}

	}

	/**
	 * Handle a remove role from group request.
	 * 
	 * @param ac
	 * @param groupUuid
	 *            Group Uuid from which the role should be removed.
	 * @param roleUuid
	 *            Role Uuid which should be removed from the group.
	 */
	public void handleRemoveRoleFromGroup(InternalActionContext ac, String groupUuid, String roleUuid) {
		validateParameter(roleUuid, "roleUuid");
		validateParameter(groupUuid, "groupUuid");

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				GroupDaoWrapper groupDao = tx.data().groupDao();
				RoleDaoWrapper roleDao = tx.data().roleDao();
				GroupRoot groupRoot = tx.data().groupDao();

				Group group = groupDao.loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
				Role role = roleDao.loadObjectByUuid(ac, roleUuid, READ_PERM);

				// No need to update the group if it is not assigned
				if (!groupRoot.hasRole(group, role)) {
					return;
				}

				utils.eventAction(batch -> {
					groupRoot.removeRole(group, role);
					group.setEditor(ac.getUser());
					group.setLastEditedTimestamp();
					batch.add(groupRoot.createRoleAssignmentEvent(group, role, UNASSIGNED));
					return batch;
				});

			}, () -> ac.send(NO_CONTENT));
		}
	}

	/**
	 * Read users that are assigned to the group and return a paged list.
	 * 
	 * @param ac
	 * @param groupUuid
	 *            Uuid of the group
	 */
	public void handleGroupUserList(InternalActionContext ac, String groupUuid) {
		validateParameter(groupUuid, "groupUuid");

		utils.syncTx(ac, tx -> {
			GroupRoot groupRoot = tx.data().groupDao();
			MeshAuthUser requestUser = ac.getUser();
			PagingParametersImpl pagingInfo = new PagingParametersImpl(ac);
			Group group = tx.data().groupDao().loadObjectByUuid(ac, groupUuid, READ_PERM);
			TransformablePage<? extends User> userPage = groupRoot.getVisibleUsers(group, requestUser, pagingInfo);
			return userPage.transformToRestSync(ac, 0);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Add the given user to a group.
	 * 
	 * @param ac
	 * @param groupUuid
	 *            Group uuid
	 * @param userUuid
	 *            Uuid of the user which should be added to the group
	 */
	public void handleAddUserToGroup(InternalActionContext ac, String groupUuid, String userUuid) {
		validateParameter(groupUuid, "groupUuid");
		validateParameter(userUuid, "userUuid");

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				GroupRoot groupDao = tx.data().groupDao();
				UserRoot userDao = tx.data().userDao();
				Group group = groupDao.loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
				User user = userDao.loadObjectByUuid(ac, userUuid, READ_PERM);

				// Only add the user if it is not yet assigned
				if (!groupDao.hasUser(group, user)) {
					utils.eventAction(batch -> {
						groupDao.addUser(group, user);
						batch.add(groupDao.createUserAssignmentEvent(group, user, ASSIGNED));
					});
				}
				return group.transformToRestSync(ac, 0);
			}, model -> ac.send(model, OK));
		}
	}

	/**
	 * Handle a remove user from group request.
	 * 
	 * @param ac
	 * @param groupUuid
	 *            Uuid of the group from which the user should be removed.
	 * @param userUuid
	 *            Uuid of the user which should be removed from the group.
	 */
	public void handleRemoveUserFromGroup(InternalActionContext ac, String groupUuid, String userUuid) {
		validateParameter(groupUuid, "groupUuid");
		validateParameter(userUuid, "userUuid");

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				GroupRoot groupDao = tx.data().groupDao();
				Group group = groupDao.loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
				User user = boot.get().userRoot().loadObjectByUuid(ac, userUuid, READ_PERM);

				// No need to remove the user if it is not assigned
				if (!groupDao.hasUser(group, user)) {
					return;
				}

				utils.eventAction(batch -> {
					groupDao.removeUser(group, user);
					batch.add(groupDao.createUserAssignmentEvent(group, user, UNASSIGNED));
				});
			}, () -> ac.send(NO_CONTENT));
		}
	}

}
