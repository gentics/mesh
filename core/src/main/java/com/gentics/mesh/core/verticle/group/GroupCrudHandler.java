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
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.PagingParameters;

import dagger.Lazy;
import rx.Single;

public class GroupCrudHandler extends AbstractCrudHandler<Group, GroupResponse> {

	private Lazy<BootstrapInitializer> boot;

	@Inject
	public GroupCrudHandler(Database db, Lazy<BootstrapInitializer> boot) {
		super(db);
		this.boot = boot;
	}

	@Override
	public RootVertex<Group> getRootVertex(InternalActionContext ac) {
		return boot.get().groupRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		HandlerUtilities.deleteElement(ac, () -> getRootVertex(ac), uuid);
	}

	public void handleGroupRolesList(InternalActionContext ac, String groupUuid) {
		db.asyncNoTx(() -> {
			Single<Group> obsGroup = getRootVertex(ac).loadObjectByUuid(ac, groupUuid, READ_PERM);
			PagingParameters pagingInfo = new PagingParameters(ac);
			MeshAuthUser requestUser = ac.getUser();
			Single<RestModel> obs = obsGroup.flatMap(group -> {
				try {
					PageImpl<? extends Role> rolePage = group.getRoles(requestUser, pagingInfo);
					return rolePage.transformToRest(ac, 0);
				} catch (Exception e) {
					return Single.error(e);
				}
			});
			return obs;
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

		db.asyncNoTx(() -> {
			Single<Group> obsGroup = boot.get().groupRoot().loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			Single<Role> obsRole = boot.get().roleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);

			Single<Single<GroupResponse>> obs = Single.zip(obsGroup, obsRole, (group, role) -> {
				Tuple<SearchQueueBatch, Group> tuple = db.tx(() -> {
					SearchQueueBatch batch = group.createIndexBatch(STORE_ACTION);
					group.addRole(role);
					return Tuple.tuple(batch, group);
				});
				SearchQueueBatch batch = tuple.v1();
				Group updatedGroup = tuple.v2();
				return batch.process().andThen(updatedGroup.transformToRest(ac, 0));
			});
			return obs.flatMap(x -> x);
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	public void handleRemoveRoleFromGroup(InternalActionContext ac, String groupUuid, String roleUuid) {
		validateParameter(roleUuid, "roleUuid");
		validateParameter(groupUuid, "groupUuid");

		db.asyncNoTx(() -> {
			// TODO check whether the role is actually part of the group
			Single<Group> obsGroup = getRootVertex(ac).loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			Single<Role> obsRole = boot.get().roleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);

			Single<Single<GroupResponse>> obs = Single.zip(obsGroup, obsRole, (group, role) -> {
				SearchQueueBatch sqBatch = db.tx(() -> {
					SearchQueueBatch batch = group.createIndexBatch(STORE_ACTION);
					group.removeRole(role);
					return batch;
				});

				return sqBatch.process().andThen(Single.just(null));
			});
			return Single.merge(obs);
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

		db.asyncNoTx(() -> {
			MeshAuthUser requestUser = ac.getUser();
			PagingParameters pagingInfo = new PagingParameters(ac);
			Single<Group> obsGroup = boot.get().groupRoot().loadObjectByUuid(ac, groupUuid, READ_PERM);
			return obsGroup.flatMap(group -> {
				try {
					PageImpl<? extends User> userPage = group.getVisibleUsers(requestUser, pagingInfo);
					return userPage.transformToRest(ac, 0);
				} catch (Exception e) {
					return Single.error(e);
				}
			});
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

		db.asyncNoTx(() -> {

			Single<Group> obsGroup = boot.get().groupRoot().loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			Single<User> obsUser = boot.get().userRoot().loadObjectByUuid(ac, userUuid, READ_PERM);
			Single<Single<GroupResponse>> obs = Single.zip(obsGroup, obsUser, (group, user) -> {
				Tuple<SearchQueueBatch, Group> tuple = db.tx(() -> {
					group.addUser(user);
					SearchQueueBatch batch = group.createIndexBatch(STORE_ACTION);
					return Tuple.tuple(batch, group);
				});
				SearchQueueBatch batch = tuple.v1();
				Group updatedGroup = tuple.v2();
				return batch.process().andThen(updatedGroup.transformToRest(ac, 0));
			});
			return obs.flatMap(x -> x);
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	public void handleRemoveUserFromGroup(InternalActionContext ac, String groupUuid, String userUuid) {
		validateParameter(groupUuid, "groupUuid");
		validateParameter(userUuid, "userUuid");

		db.asyncNoTx(() -> {
			Single<Group> obsGroup = boot.get().groupRoot().loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			Single<User> obsUser = boot.get().userRoot().loadObjectByUuid(ac, userUuid, READ_PERM);
			return Single.zip(obsUser, obsGroup, (user, group) -> {
				Tuple<SearchQueueBatch, Group> tuple = db.tx(() -> {
					SearchQueueBatch batch = group.createIndexBatch(STORE_ACTION);
					batch.addEntry(user, STORE_ACTION);
					group.removeUser(user);
					return Tuple.tuple(batch, group);
				});
				SearchQueueBatch batch = tuple.v1();
				return batch.process().andThen(Single.just(null));
			}).flatMap(x -> x);
		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
	}

}
