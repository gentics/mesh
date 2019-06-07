package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_GROUP;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.syncleus.ferma.index.EdgeIndexDefinition.edgeIndex;
import static com.syncleus.ferma.type.EdgeTypeDefinition.edgeType;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.IndexHandler;
import com.gentics.mesh.graphdb.spi.TypeHandler;

/**
 * @see GroupRoot
 */
public class GroupRootImpl extends AbstractRootVertex<Group> implements GroupRoot {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(GroupRootImpl.class, MeshVertexImpl.class);
		type.createType(edgeType(HAS_GROUP));
		index.createIndex(edgeIndex(HAS_GROUP).withInOut().withOut());
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
	public Group create(String name, User creator, String uuid) {
		GroupImpl group = getGraph().addFramedVertex(GroupImpl.class);
		if (uuid != null) {
			group.setUuid(uuid);
		}
		group.setName(name);
		group.setCreated(creator);
		addGroup(group);

		return group;
	}

	@Override
	public void delete(BulkActionContext bac) {
		throw new NotImplementedException("The group root node can't be deleted");
	}

	@Override
	public Group create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		MeshAuthUser requestUser = ac.getUser();
		GroupCreateRequest requestModel = ac.fromJson(GroupCreateRequest.class);

		if (StringUtils.isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}
		if (!requestUser.hasPermission(this, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", this.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		MeshRoot root = MeshInternal.get().boot().meshRoot();

		// Check whether a group with the same name already exists
		Group groupWithSameName = findByName(requestModel.getName());
		if (groupWithSameName != null && !groupWithSameName.getUuid().equals(getUuid())) {
			throw conflict(groupWithSameName.getUuid(), requestModel.getName(), "group_conflicting_name", requestModel.getName());
		}

		// Finally create the group and set the permissions
		Group group = create(requestModel.getName(), requestUser, uuid);
		requestUser.addCRUDPermissionOnRole(root.getGroupRoot(), CREATE_PERM, group);
		batch.add(group.onCreated());
		return group;
	}

}
