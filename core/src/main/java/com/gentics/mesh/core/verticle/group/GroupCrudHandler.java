package com.gentics.mesh.core.verticle.group;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.elasticsearch.common.collect.Tuple;
import org.springframework.stereotype.Component;

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
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;

import rx.Observable;

@Component
public class GroupCrudHandler extends AbstractCrudHandler<Group, GroupResponse> {

	@Override
	public RootVertex<Group> getRootVertex(InternalActionContext ac) {
		return boot.groupRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> getRootVertex(ac), "uuid", "group_deleted");
	}

	public void handleGroupRolesList(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			Observable<Group> obsGroup = getRootVertex(ac).loadObject(ac, "groupUuid", READ_PERM);
			PagingParameter pagingInfo = ac.getPagingParameter();
			MeshAuthUser requestUser = ac.getUser();
			Observable<RestModel> obs = obsGroup.flatMap(group -> {
				try {
					PageImpl<? extends Role> rolePage = group.getRoles(requestUser, pagingInfo);
					return rolePage.transformToRest(ac);
				} catch (Exception e) {
					return Observable.error(e);
				}
			});
			return obs.toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleAddRoleToGroup(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			Observable<Group> obsGroup = boot.groupRoot().loadObject(ac, "groupUuid", UPDATE_PERM);
			Observable<Role> obsRole = boot.roleRoot().loadObject(ac, "roleUuid", READ_PERM);

			Observable<Observable<GroupResponse>> obs = Observable.zip(obsGroup, obsRole, (group, role) -> {
				Tuple<SearchQueueBatch, Group> tuple = db.trx(() -> {
					SearchQueueBatch batch = group.addIndexBatch(UPDATE_ACTION);
					group.addRole(role);
					return Tuple.tuple(batch, group);
				});
				SearchQueueBatch batch = tuple.v1();
				Group updatedGroup = tuple.v2();
				return batch.process().flatMap(done -> {
					return updatedGroup.transformToRest(ac);
				});
			});

			return obs.flatMap(x -> x).toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

	public void handleRemoveRoleFromGroup(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			// TODO check whether the role is actually part of the group
			Observable<Group> obsGroup = getRootVertex(ac).loadObject(ac, "groupUuid", UPDATE_PERM);
			Observable<Role> obsRole = boot.roleRoot().loadObject(ac, "roleUuid", READ_PERM);

			return Observable.zip(obsGroup, obsRole, (group, role) -> {

				Tuple<SearchQueueBatch, Group> tuple = db.trx(() -> {
					SearchQueueBatch batch = group.addIndexBatch(UPDATE_ACTION);
					group.removeRole(role);
					return Tuple.tuple(batch, group);
				});

				SearchQueueBatch batch = tuple.v1();
				Group updatedGroup = tuple.v2();

				return batch.process().map(done -> {
					return updatedGroup.transformToRest(ac);
				}).flatMap(x -> x).toBlocking().first();
			}).toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleGroupUserList(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			MeshAuthUser requestUser = ac.getUser();
			PagingParameter pagingInfo = ac.getPagingParameter();
			Observable<Group> obsGroup = boot.groupRoot().loadObject(ac, "groupUuid", READ_PERM);
			return obsGroup.flatMap(group -> {
				try {
					PageImpl<? extends User> userPage = group.getVisibleUsers(requestUser, pagingInfo);
					return userPage.transformToRest(ac);
				} catch (Exception e) {
					return Observable.error(e);
				}
			}).toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleAddUserToGroup(InternalActionContext ac) {
		db.asyncNoTrx(() -> {

			Observable<Group> obsGroup = boot.groupRoot().loadObject(ac, "groupUuid", UPDATE_PERM);
			Observable<User> obsUser = boot.userRoot().loadObject(ac, "userUuid", READ_PERM);
			return Observable.zip(obsGroup, obsUser, (group, user) -> {
				Tuple<SearchQueueBatch, Group> tuple = db.trx(() -> {
					SearchQueueBatch batch = group.addIndexBatch(UPDATE_ACTION);
					batch.addEntry(user, UPDATE_ACTION);
					group.addUser(user);
					return Tuple.tuple(batch, group);
				});
				// BUG Add SQB processing
				return tuple.v2().transformToRest(ac);
			}).flatMap(x -> x).toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleRemoveUserFromGroup(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			Observable<Group> obsGroup = boot.groupRoot().loadObject(ac, "groupUuid", UPDATE_PERM);
			Observable<User> obsUser = boot.userRoot().loadObject(ac, "userUuid", READ_PERM);
			return Observable.zip(obsUser, obsGroup, (user, group) -> {
				Tuple<SearchQueueBatch, Group> tuple = db.trx(() -> {
					SearchQueueBatch batch = group.addIndexBatch(UPDATE_ACTION);
					batch.addEntry(user, UPDATE_ACTION);
					group.removeUser(user);
					return Tuple.tuple(batch, group);
				});
				// BUG Add SQB processing
				return tuple.v2().transformToRest(ac);
			}).flatMap(x -> x).toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

}
