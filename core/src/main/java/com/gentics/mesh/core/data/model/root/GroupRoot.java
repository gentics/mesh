package com.gentics.mesh.core.data.model.root;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_GROUP;

import java.util.List;

import com.gentics.mesh.core.data.model.Group;
import com.gentics.mesh.core.data.model.generic.MeshVertex;

public class GroupRoot extends MeshVertex {

	// TODO unique node

	public List<? extends Group> getGroups() {
		return out(HAS_GROUP).toList(Group.class);
	}

	public void addGroup(Group group) {
		linkOut(group, HAS_GROUP);
	}

	public Group create(String name) {
		Group group = getGraph().addFramedVertex(Group.class);
		group.setName(name);
		addGroup(group);
		return group;
	}

}
