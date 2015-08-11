package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_GROUP;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.root.GroupRoot;

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

}
