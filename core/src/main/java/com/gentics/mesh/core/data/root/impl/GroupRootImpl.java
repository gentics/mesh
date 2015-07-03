package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_GROUP;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.root.GroupRoot;

public class GroupRootImpl extends AbstractRootVertex<Group> implements GroupRoot {

	// TODO unique node

	protected Class<? extends Group> getPersistanceClass() {
		return GroupImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_GROUP;
	}

	@Override
	public void addGroup(Group group) {
		linkOut(group.getImpl(), HAS_GROUP);
	}

	@Override
	public Group create(String name) {
		GroupImpl group = getGraph().addFramedVertex(GroupImpl.class);
		group.setName(name);
		addGroup(group);
		return group;
	}

	@Override
	public GroupRootImpl getImpl() {
		return this;
	}

}
