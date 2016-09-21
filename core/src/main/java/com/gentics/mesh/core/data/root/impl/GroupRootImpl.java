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
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;

public class GroupRootImpl extends AbstractRootVertex<Group> implements GroupRoot {

	public static void init(Database database) {
		database.addVertexType(GroupRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_GROUP, true, false, true);
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
	public Group create(InternalActionContext ac, SearchQueueBatch batch) {
		MeshAuthUser requestUser = ac.getUser();
		GroupCreateRequest requestModel = ac.fromJson(GroupCreateRequest.class);

		BootstrapInitializer boot = MeshInternal.get().boot();

		if (StringUtils.isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}
		if (!requestUser.hasPermission(this, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", this.getUuid());
		}
		MeshRoot root = boot.meshRoot();

		Group groupWithSameName = findByName(requestModel.getName());
		if (groupWithSameName != null && !groupWithSameName.getUuid().equals(getUuid())) {
			throw conflict(groupWithSameName.getUuid(), requestModel.getName(), "group_conflicting_name", requestModel.getName());
		}
		requestUser.reload();
		Group group = create(requestModel.getName(), requestUser);
		requestUser.addCRUDPermissionOnRole(root.getGroupRoot(), CREATE_PERM, group);
		group.addIndexBatchEntry(batch, STORE_ACTION);
		return group;
	}

}
