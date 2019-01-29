package com.gentics.mesh.core.endpoint.group;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.Optional;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.EventQueueBatch;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.util.ResultInfo;
import com.gentics.mesh.util.Tuple;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.reactivex.Single;

/**
 * Handler for group specific request methods.
 */
public class GroupCrudHandler extends AbstractCrudHandler<Group, GroupResponse> {

	private static final Logger log = LoggerFactory.getLogger(GroupCrudHandler.class);

	private Lazy<BootstrapInitializer> boot;

	private SearchQueue searchQueue;

	@Inject
	public GroupCrudHandler(Database db, Lazy<BootstrapInitializer> boot, SearchQueue searchQueue, HandlerUtilities utils) {
		super(db, utils);
		this.boot = boot;
		this.searchQueue = searchQueue;
	}

	@Override
	public RootVertex<Group> getRootVertex(InternalActionContext ac) {
		return boot.get().groupRoot();
	}

	/**
	 * Handle a read roles of group request.
	 * 
	 * @param ac
	 * @param groupUuid
	 *            Group Uuid from which the roles should be loaded
	 */
	public void handleGroupRolesList(InternalActionContext ac, String groupUuid) {
		db.asyncTx(() -> {
			Group group = getRootVertex(ac).loadObjectByUuid(ac, groupUuid, READ_PERM);
			PagingParametersImpl pagingInfo = new PagingParametersImpl(ac);
			TransformablePage<? extends Role> rolePage = group.getRoles(ac.getUser(), pagingInfo);
			return rolePage.transformToRest(ac, 0);
		}).subscribe(model -> ac.send(model, OK), ac::fail);
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

		db.asyncTx(() -> {
			Group group = boot.get().groupRoot().loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			Role role = boot.get().roleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);
			// Handle idempotency
			Tuple<Group, EventQueueBatch> tuple;
			if (group.hasRole(role)) {
				if (log.isDebugEnabled()) {
					log.debug("Role {" + role.getUuid() + "} is already assigned to group {" + group.getUuid() + "}.");
				}
				tuple = Tuple.tuple(group, searchQueue.create());
			} else {
				tuple = db.tx(() -> {
					EventQueueBatch batch = searchQueue.create();
					group.addRole(role);
					group.setEditor(ac.getUser());
					group.setLastEditedTimestamp();
					// No need to update users as well. Those documents are not affected by this modification
					batch.store(group, false);
					return Tuple.tuple(group, batch);
				});
			}
			return tuple.v2().processAsync().andThen(tuple.v1().transformToRest(ac, 0));
		}).subscribe(model -> ac.send(model, OK), ac::fail);

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

		db.asyncTx(() -> {
			// TODO check whether the role is actually part of the group
			Group group = getRootVertex(ac).loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			Role role = boot.get().roleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);

			return db.tx(() -> {
				EventQueueBatch batch = searchQueue.create();
				group.removeRole(role);
				group.setEditor(ac.getUser());
				group.setLastEditedTimestamp();
				// No need to update users as well. Those documents are not affected by this modification
				batch.store(group, false);
				return batch;
			}).processAsync().andThen(Single.just(Optional.empty()));

		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
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

		db.asyncTx(() -> {
			MeshAuthUser requestUser = ac.getUser();
			PagingParametersImpl pagingInfo = new PagingParametersImpl(ac);
			Group group = boot.get().groupRoot().loadObjectByUuid(ac, groupUuid, READ_PERM);
			TransformablePage<? extends User> userPage = group.getVisibleUsers(requestUser, pagingInfo);
			return userPage.transformToRest(ac, 0);
		}).subscribe(model -> ac.send(model, OK), ac::fail);
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

		db.asyncTx(() -> {
			Group group = boot.get().groupRoot().loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			User user = boot.get().userRoot().loadObjectByUuid(ac, userUuid, READ_PERM);
			ResultInfo info = db.tx(() -> {
				EventQueueBatch batch = searchQueue.create();
				group.addUser(user);
				batch.store(group, true);
				GroupResponse model = group.transformToRestSync(ac, 0);
				return new ResultInfo(model, batch);
			});
			return info.getBatch().processAsync().andThen(Single.just(info.getModel()));
		}).subscribe(model -> ac.send(model, OK), ac::fail);

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

		db.asyncTx(() -> {
			Group group = boot.get().groupRoot().loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			User user = boot.get().userRoot().loadObjectByUuid(ac, userUuid, READ_PERM);

			return db.tx(() -> {
				EventQueueBatch batch = searchQueue.create();
				batch.store(group, true);
				batch.store(user, false);
				group.removeUser(user);
				return batch;
			}).processAsync().toSingleDefault(Optional.empty());
		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
	}

}
