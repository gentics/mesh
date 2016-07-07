package com.gentics.mesh.core.verticle.group;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.elasticsearch.common.collect.Tuple;
import org.springframework.stereotype.Component;

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
import com.gentics.mesh.parameter.impl.PagingParameters;

import rx.Observable;

@Component
public class GroupCrudHandler extends AbstractCrudHandler<Group, GroupResponse> {

	@Override
	public RootVertex<Group> getRootVertex(InternalActionContext ac) {
		return boot.groupRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		HandlerUtilities.deleteElement(ac, () -> getRootVertex(ac), uuid, "group_deleted");
	}

	public void handleGroupRolesList(InternalActionContext ac, String groupUuid) {
		db.asyncNoTrxExperimental(() -> {
			Observable<Group> obsGroup = getRootVertex(ac).loadObjectByUuid(ac, groupUuid, READ_PERM);
			PagingParameters pagingInfo = new PagingParameters(ac);
			MeshAuthUser requestUser = ac.getUser();
			Observable<RestModel> obs = obsGroup.flatMap(group -> {
				try {
					PageImpl<? extends Role> rolePage = group.getRoles(requestUser, pagingInfo);
					return rolePage.transformToRest(ac, 0);
				} catch (Exception e) {
					return Observable.error(e);
				}
			});
			return obs;
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
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

		db.asyncNoTrxExperimental(() -> {
			Observable<Group> obsGroup = boot.groupRoot().loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			Observable<Role> obsRole = boot.roleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);

			Observable<Observable<GroupResponse>> obs = Observable.zip(obsGroup, obsRole, (group, role) -> {
				Tuple<SearchQueueBatch, Group> tuple = db.trx(() -> {
					SearchQueueBatch batch = group.createIndexBatch(STORE_ACTION);
					group.addRole(role);
					return Tuple.tuple(batch, group);
				});
				SearchQueueBatch batch = tuple.v1();
				Group updatedGroup = tuple.v2();
				return batch.process().flatMap(done -> {
					return updatedGroup.transformToRest(ac, 0);
				});
			});

			return obs.flatMap(x -> x);
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

	public void handleRemoveRoleFromGroup(InternalActionContext ac, String groupUuid, String roleUuid) {
		validateParameter(roleUuid, "roleUuid");
		validateParameter(groupUuid, "groupUuid");

		db.asyncNoTrxExperimental(() -> {
			// TODO check whether the role is actually part of the group
			Observable<Group> obsGroup = getRootVertex(ac).loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			Observable<Role> obsRole = boot.roleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);

			return Observable.zip(obsGroup, obsRole, (group, role) -> {

				Tuple<SearchQueueBatch, Group> tuple = db.trx(() -> {
					SearchQueueBatch batch = group.createIndexBatch(STORE_ACTION);
					group.removeRole(role);
					return Tuple.tuple(batch, group);
				});

				SearchQueueBatch batch = tuple.v1();
				Group updatedGroup = tuple.v2();

				return batch.process().map(done -> {
					return updatedGroup.transformToRest(ac, 0);
				}).flatMap(x -> x).toBlocking().last();
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
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

		db.asyncNoTrxExperimental(() -> {
			MeshAuthUser requestUser = ac.getUser();
			PagingParameters pagingInfo = new PagingParameters(ac);
			Observable<Group> obsGroup = boot.groupRoot().loadObjectByUuid(ac, groupUuid, READ_PERM);
			return obsGroup.flatMap(group -> {
				try {
					PageImpl<? extends User> userPage = group.getVisibleUsers(requestUser, pagingInfo);
					return userPage.transformToRest(ac, 0);
				} catch (Exception e) {
					return Observable.error(e);
				}
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
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

		db.asyncNoTrxExperimental(() -> {

			Observable<Group> obsGroup = boot.groupRoot().loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			Observable<User> obsUser = boot.userRoot().loadObjectByUuid(ac, userUuid, READ_PERM);
			Observable<Observable<GroupResponse>> obs = Observable.zip(obsGroup, obsUser, (group, user) -> {
				Tuple<SearchQueueBatch, Group> tuple = db.trx(() -> {
					group.addUser(user);
					SearchQueueBatch batch = group.createIndexBatch(STORE_ACTION);
					return Tuple.tuple(batch, group);
				});
				SearchQueueBatch batch = tuple.v1();
				Group updatedGroup = tuple.v2();
				return batch.process().flatMap(i -> updatedGroup.transformToRest(ac, 0));
			});
			return obs.flatMap(x -> x);
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleRemoveUserFromGroup(InternalActionContext ac, String groupUuid, String userUuid) {
		validateParameter(groupUuid, "groupUuid");
		validateParameter(userUuid, "userUuid");

		db.asyncNoTrxExperimental(() -> {
			Observable<Group> obsGroup = boot.groupRoot().loadObjectByUuid(ac, groupUuid, UPDATE_PERM);
			Observable<User> obsUser = boot.userRoot().loadObjectByUuid(ac, userUuid, READ_PERM);
			return Observable.zip(obsUser, obsGroup, (user, group) -> {
				Tuple<SearchQueueBatch, Group> tuple = db.trx(() -> {
					SearchQueueBatch batch = group.createIndexBatch(STORE_ACTION);
					batch.addEntry(user, STORE_ACTION);
					group.removeUser(user);
					return Tuple.tuple(batch, group);
				});
				// BUG Add SQB processing
				return tuple.v2().transformToRest(ac, 0);
			}).flatMap(x -> x);
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

}
