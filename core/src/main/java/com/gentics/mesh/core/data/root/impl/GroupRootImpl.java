package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_GROUP;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.dagger.MeshCore;
import com.gentics.mesh.graphdb.spi.Database;

import rx.Single;

public class GroupRootImpl extends AbstractRootVertex<Group> implements GroupRoot {

	public static void init(Database database) {
		database.addVertexType(GroupRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_GROUP);
	}

	@Override
	public Class<? extends Group> getPersistanceClass() {
		return GroupImpl.class;
	}

	@Override
	public String getRootLabel() {
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
		group.setCreated(creator);
		addGroup(group);

		return group;
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		throw new NotImplementedException("The group root node can't be deleted");
	}

	@Override
	public Single<Group> create(InternalActionContext ac) {
		MeshAuthUser requestUser = ac.getUser();
		GroupCreateRequest requestModel = ac.fromJson(GroupCreateRequest.class);

		Database db = MeshCore.get().database();
		BootstrapInitializer boot = MeshCore.get().boot();

		if (StringUtils.isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}

		return db.noTx(() -> {
			MeshRoot root = boot.meshRoot();
			if (requestUser.hasPermissionSync(ac, this, CREATE_PERM)) {
				Group groupWithSameName = findByName(requestModel.getName()).toBlocking().value();
				if (groupWithSameName != null && !groupWithSameName.getUuid().equals(getUuid())) {
					throw conflict(groupWithSameName.getUuid(), requestModel.getName(), "group_conflicting_name", requestModel.getName());
				}
				Tuple<SearchQueueBatch, Group> tuple = db.tx(() -> {
					requestUser.reload();
					Group group = create(requestModel.getName(), requestUser);
					requestUser.addCRUDPermissionOnRole(root.getGroupRoot(), CREATE_PERM, group);
					SearchQueueBatch batch = group.createIndexBatch(STORE_ACTION);
					return Tuple.tuple(batch, group);
				});
				SearchQueueBatch batch = tuple.v1();
				Group group = tuple.v2();
				return batch.process().andThen(Single.just(group));
			} else {
				throw error(FORBIDDEN, "error_missing_perm", this.getUuid());
			}

		});
	}

}
