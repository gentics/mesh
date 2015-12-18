package com.gentics.mesh.core.verticle.group;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.transformAndRespond;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.elasticsearch.common.collect.Tuple;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.VerticleHelper;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

@Component
public class GroupCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(InternalActionContext ac) {
		createElement(ac, () -> boot.groupRoot());
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> boot.groupRoot(), "uuid", "group_deleted");
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		updateElement(ac, "uuid", () -> boot.groupRoot());
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		readElement(ac, "uuid", () -> boot.groupRoot());
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		readElementList(ac, () -> boot.groupRoot());
	}

	public void handleGroupRolesList(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			PagingParameter pagingInfo = ac.getPagingParameter();
			MeshAuthUser requestUser = ac.getUser();
			boot.groupRoot().loadObject(ac, "groupUuid", READ_PERM, grh -> {
				try {
					Page<? extends Role> rolePage = grh.result().getRoles(requestUser, pagingInfo);
					rolePage.transformAndRespond(ac, OK);
				} catch (InvalidArgumentException e) {
					ac.fail(e);
				}
			});
		} , ac.errorHandler());
	}

	public void handleAddRoleToGroup(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			boot.groupRoot().loadObject(ac, "groupUuid", UPDATE_PERM, grh -> {
				if (hasSucceeded(ac, grh)) {
					boot.roleRoot().loadObject(ac, "roleUuid", READ_PERM, rrh -> {
						if (hasSucceeded(ac, rrh)) {
							Group group = grh.result();
							Role role = rrh.result();
							db.trx(txAdd -> {
								SearchQueueBatch batch = group.addIndexBatch(UPDATE_ACTION);
								group.addRole(role);
								txAdd.complete(Tuple.tuple(batch, group));
							} , (AsyncResult<Tuple<SearchQueueBatch, Group>> txAdded) -> {
								if (txAdded.failed()) {
									ac.errorHandler().handle(Future.failedFuture(txAdded.cause()));
								} else {
									VerticleHelper.processOrFail(ac, txAdded.result().v1(), ch -> {
										transformAndRespond(ac, txAdded.result().v2(), OK);
									} , txAdded.result().v2());

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
			boot.groupRoot().loadObject(ac, "groupUuid", UPDATE_PERM, grh -> {
				if (hasSucceeded(ac, grh)) {
					// TODO check whether the role is actually part of the group
					boot.roleRoot().loadObject(ac, "roleUuid", READ_PERM, rrh -> {
						if (hasSucceeded(ac, rrh)) {
							Group group = grh.result();
							Role role = rrh.result();
							db.trx(txRemove -> {
								SearchQueueBatch batch = group.addIndexBatch(UPDATE_ACTION);
								group.removeRole(role);
								txRemove.complete(Tuple.tuple(batch, group));
							} , (AsyncResult<Tuple<SearchQueueBatch, Group>> txRemoved) -> {
								if (txRemoved.failed()) {
									ac.errorHandler().handle(Future.failedFuture(txRemoved.cause()));
								} else {
									transformAndRespond(ac, txRemoved.result().v2(), OK);
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
			PagingParameter pagingInfo = ac.getPagingParameter();
			boot.groupRoot().loadObject(ac, "groupUuid", READ_PERM, grh -> {
				if (hasSucceeded(ac, grh)) {
					try {
						Group group = grh.result();
						Page<? extends User> userPage = group.getVisibleUsers(requestUser, pagingInfo);
						userPage.transformAndRespond(ac, OK);
					} catch (Exception e) {
						ac.fail(e);
					}
				}
			});
		} , ac.errorHandler());
	}

	public void handleAddUserToGroup(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {

			boot.groupRoot().loadObject(ac, "groupUuid", UPDATE_PERM, grh -> {
				if (hasSucceeded(ac, grh)) {
					boot.userRoot().loadObject(ac, "userUuid", READ_PERM, urh -> {
						if (hasSucceeded(ac, urh)) {
							db.trx(txAdd -> {
								Group group = grh.result();
								SearchQueueBatch batch = group.addIndexBatch(UPDATE_ACTION);
								User user = urh.result();
								batch.addEntry(user, UPDATE_ACTION);
								group.addUser(user);
								txAdd.complete(Tuple.tuple(batch, group));
							} , (AsyncResult<Tuple<SearchQueueBatch, Group>> txAdded) -> {
								if (txAdded.failed()) {
									ac.errorHandler().handle(Future.failedFuture(txAdded.cause()));
								} else {
									transformAndRespond(ac, txAdded.result().v2(), OK);
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
			boot.groupRoot().loadObject(ac, "groupUuid", UPDATE_PERM, grh -> {
				if (hasSucceeded(ac, grh)) {
					boot.userRoot().loadObject(ac, "userUuid", READ_PERM, urh -> {
						if (hasSucceeded(ac, urh)) {
							db.trx(tcRemove -> {
								Group group = grh.result();
								SearchQueueBatch batch = group.addIndexBatch(UPDATE_ACTION);
								User user = urh.result();
								batch.addEntry(user, UPDATE_ACTION);
								group.removeUser(user);
								tcRemove.complete(Tuple.tuple(batch, group));
							} , (AsyncResult<Tuple<SearchQueueBatch, Group>> txRemoved) -> {
								if (txRemoved.failed()) {
									ac.errorHandler().handle(Future.failedFuture(txRemoved.cause()));
								} else {
									transformAndRespond(ac, txRemoved.result().v2(), OK);
								}
							});
						}
					});
				}
			});
		} , ac.errorHandler());
	}

}
