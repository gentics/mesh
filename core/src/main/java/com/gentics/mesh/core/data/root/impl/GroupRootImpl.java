package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_GROUP;

import java.util.List;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.root.GroupRoot;

public class GroupRootImpl extends MeshVertexImpl implements GroupRoot {

	// TODO unique node

	public List<? extends Group> getGroups() {
		return out(HAS_GROUP).has(GroupImpl.class).toList(GroupImpl.class);
	}

	public void addGroup(GroupImpl group) {
		linkOut(group, HAS_GROUP);
	}

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
