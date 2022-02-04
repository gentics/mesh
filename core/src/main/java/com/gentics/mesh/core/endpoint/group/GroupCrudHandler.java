package com.gentics.mesh.core.endpoint.group;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.GroupDAOActions;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.PageTransformer;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler for group specific request methods.
 */
public class GroupCrudHandler extends AbstractCrudHandler<HibGroup, GroupResponse> {

	private static final Logger log = LoggerFactory.getLogger(GroupCrudHandler.class);

	private final PageTransformer pageTransformer;

	@Inject
	public GroupCrudHandler(Database db, HandlerUtilities utils, WriteLock writeLock, GroupDAOActions groupActions, PageTransformer pageTransformer) {
		super(db, utils, writeLock, groupActions);
		this.pageTransformer = pageTransformer;
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
			GroupDao groupDao = tx.groupDao();
			HibGroup group = groupDao.loadObjectByUuid(ac, groupUuid, READ_PERM);
			PagingParametersImpl pagingInfo = new PagingParametersImpl(ac);
			Page<? extends HibRole> rolePage = groupDao.getRoles(group, ac.getUser(), pagingInfo);
			return pageTransformer.transformToRestSync(rolePage, ac, 0);
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
			utils.syncTx(ac, (batch, tx) -> {
				GroupDao groupDao = tx.groupDao();
				RoleDao roleDao = tx.roleDao();

				HibGroup group = groupDao.loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
				HibRole role = roleDao.loadObjectByUuid(ac, roleUuid, READ_PERM);
				// Handle idempotency
				if (groupDao.hasRole(group, role)) {
					if (log.isDebugEnabled()) {
						log.debug("Role {" + role.getUuid() + "} is already assigned to group {" + group.getUuid() + "}.");
					}
				} else {
					groupDao.addRole(group, role);
					group.setEditor(ac.getUser());
					group.setLastEditedTimestamp();
					batch.add(groupDao.createRoleAssignmentEvent(group, role, ASSIGNED));
				}
				return groupDao.transformToRestSync(group, ac, 0);
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
			utils.syncTx(ac, (batch, tx) -> {
				GroupDao groupDao = tx.groupDao();
				RoleDao roleDao = tx.roleDao();

				HibGroup group = groupDao.loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
				HibRole role = roleDao.loadObjectByUuid(ac, roleUuid, READ_PERM);

				// No need to update the group if it is not assigned
				if (!groupDao.hasRole(group, role)) {
					return;
				}

				groupDao.removeRole(group, role);
				group.setEditor(ac.getUser());
				group.setLastEditedTimestamp();
				batch.add(groupDao.createRoleAssignmentEvent(group, role, UNASSIGNED));
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
			GroupDao groupDao = tx.groupDao();
			HibUser requestUser = ac.getUser();
			PagingParametersImpl pagingInfo = new PagingParametersImpl(ac);
			HibGroup group = groupDao.loadObjectByUuid(ac, groupUuid, READ_PERM);
			Page<? extends HibUser> userPage = groupDao.getVisibleUsers(group, requestUser, pagingInfo);
			return pageTransformer.transformToRestSync(userPage, ac, 0);
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
			utils.syncTx(ac, (batch, tx) -> {
				GroupDao groupDao = tx.groupDao();
				UserDao userDao = tx.userDao();
				HibGroup group = groupDao.loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
				HibUser user = userDao.loadObjectByUuid(ac, userUuid, READ_PERM);

				// Only add the user if it is not yet assigned
				if (!groupDao.hasUser(group, user)) {
					groupDao.addUser(group, user);
					batch.add(groupDao.createUserAssignmentEvent(group, user, ASSIGNED));
				}
				return groupDao.transformToRestSync(group, ac, 0);
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
			utils.syncTx(ac, (batch, tx) -> {
				GroupDao groupDao = tx.groupDao();
				UserDao userDao = tx.userDao();

				HibGroup group = groupDao.loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
				HibUser user = userDao.loadObjectByUuid(ac, userUuid, READ_PERM);

				// No need to remove the user if it is not assigned
				if (!groupDao.hasUser(group, user)) {
					return;
				}

				groupDao.removeUser(group, user);
				batch.add(groupDao.createUserAssignmentEvent(group, user, UNASSIGNED));
			}, () -> ac.send(NO_CONTENT));
		}
	}

}
