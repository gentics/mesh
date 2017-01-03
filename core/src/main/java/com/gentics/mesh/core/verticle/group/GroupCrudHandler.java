package com.gentics.mesh.core.verticle.group;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;

import dagger.Lazy;
import rx.Single;

/**
 * Handler for group specific request methods.
 */
public class GroupCrudHandler extends AbstractCrudHandler<Group, GroupResponse> {

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
		db.operateNoTx(() -> {
			Group group = getRootVertex(ac).loadObjectByUuid(ac, groupUuid, READ_PERM);
			PagingParametersImpl pagingInfo = new PagingParametersImpl(ac);
			MeshAuthUser requestUser = ac.getUser();
			Page<? extends Role> rolePage = group.getRoles(requestUser, pagingInfo);
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

		db.operateNoTx(() -> {
			Group group = boot.get().groupRoot().loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			Role role = boot.get().roleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);

			Tuple<SearchQueueBatch, Group> tuple = db.tx(() -> {
				SearchQueueBatch batch = searchQueue.createBatch();
				group.addIndexBatchEntry(batch, STORE_ACTION, true);
				group.addRole(role);
				return Tuple.tuple(batch, group);
			});
			SearchQueueBatch batch = tuple.v1();
			Group updatedGroup = tuple.v2();
			return batch.processAsync().andThen(updatedGroup.transformToRest(ac, 0));
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

		db.operateNoTx(() -> {
			// TODO check whether the role is actually part of the group
			Group group = getRootVertex(ac).loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			Role role = boot.get().roleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);

			SearchQueueBatch sqBatch = db.tx(() -> {
				SearchQueueBatch batch = searchQueue.createBatch();
				group.addIndexBatchEntry(batch, STORE_ACTION, true);
				group.removeRole(role);
				return batch;
			});

			return sqBatch.processAsync().andThen(Single.just(null));
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

		db.operateNoTx(() -> {
			MeshAuthUser requestUser = ac.getUser();
			PagingParametersImpl pagingInfo = new PagingParametersImpl(ac);
			Group group = boot.get().groupRoot().loadObjectByUuid(ac, groupUuid, READ_PERM);
			Page<? extends User> userPage = group.getVisibleUsers(requestUser, pagingInfo);
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

		db.operateNoTx(() -> {
			Group group = boot.get().groupRoot().loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			User user = boot.get().userRoot().loadObjectByUuid(ac, userUuid, READ_PERM);
			Tuple<SearchQueueBatch, Group> tuple = db.tx(() -> {
				group.addUser(user);
				SearchQueueBatch batch = searchQueue.createBatch();
				group.addIndexBatchEntry(batch, STORE_ACTION, true);
				return Tuple.tuple(batch, group);
			});
			SearchQueueBatch batch = tuple.v1();
			Group updatedGroup = tuple.v2();
			return batch.processAsync().andThen(updatedGroup.transformToRest(ac, 0));
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

		db.operateNoTx(() -> {
			Group group = boot.get().groupRoot().loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			User user = boot.get().userRoot().loadObjectByUuid(ac, userUuid, READ_PERM);
			Tuple<SearchQueueBatch, Group> tuple = db.tx(() -> {
				SearchQueueBatch batch = searchQueue.createBatch();
				group.addIndexBatchEntry(batch, STORE_ACTION, true);
				batch.addEntry(user, STORE_ACTION);
				group.removeUser(user);
				return Tuple.tuple(batch, group);
			});
			SearchQueueBatch batch = tuple.v1();
			return batch.processAsync().andThen(Single.just(null));
		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
	}

}
