package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_GROUP;
import static com.gentics.mesh.util.VerticleHelper.processOrFail;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class GroupRootImpl extends AbstractRootVertex<Group>implements GroupRoot {

	@Override
	protected Class<? extends Group> getPersistanceClass() {
		return GroupImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_GROUP;
	}

	@Override
	public void addGroup(Group group) {
		addItem(group);
	}

	@Override
	public void removeGroup(Group group) {
		removeItem(group);
	}

	@Override
	public Group create(String name, User creator) {
		GroupImpl group = getGraph().addFramedVertex(GroupImpl.class);
		group.setName(name);
		addGroup(group);

		group.setCreator(creator);
		group.setCreationTimestamp(System.currentTimeMillis());
		group.setEditor(creator);
		group.setLastEditedTimestamp(System.currentTimeMillis());

		return group;
	}

	@Override
	public void delete() {
		throw new NotImplementedException("The group root node can't be deleted");
	}

	@Override
	public void create(InternalActionContext ac, Handler<AsyncResult<Group>> handler) {
		MeshAuthUser requestUser = ac.getUser();
		GroupCreateRequest requestModel = ac.fromJson(GroupCreateRequest.class);

		Database db = MeshSpringConfiguration.getInstance().database();
		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		if (StringUtils.isEmpty(requestModel.getName())) {
			handler.handle(ac.failedFuture(BAD_REQUEST, "error_name_must_be_set"));
			return;
		}
		db.noTrx(noTrx -> {
			MeshRoot root = boot.meshRoot();
			if (requestUser.hasPermission(ac, this, CREATE_PERM)) {
				if (findByName(requestModel.getName()) != null) {
					handler.handle(ac.failedFuture(CONFLICT, "group_conflicting_name", requestModel.getName()));
					return;
				}
				db.blockingTrx(txCreate -> {
					requestUser.reload();
					Group group = create(requestModel.getName(), requestUser);
					requestUser.addCRUDPermissionOnRole(root.getGroupRoot(), CREATE_PERM, group);
					SearchQueueBatch batch = group.addIndexBatch(SearchQueueEntryAction.CREATE_ACTION);
					txCreate.complete(Tuple.tuple(batch, group));
				} , (AsyncResult<Tuple<SearchQueueBatch, Group>> txCreated) -> {
					if (txCreated.failed()) {
						handler.handle(Future.failedFuture(txCreated.cause()));
					} else {
						processOrFail(ac, txCreated.result().v1(), handler, txCreated.result().v2());
					}
				});

				return;
			} else {
				handler.handle(Future.failedFuture(new InvalidPermissionException(ac.i18n("error_missing_perm", this.getUuid()))));
				return;
			}

		});

	}

}
