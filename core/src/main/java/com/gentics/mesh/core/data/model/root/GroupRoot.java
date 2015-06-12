package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Group;

public class GroupRoot extends MeshVertex {

	// TODO unique node

	public List<? extends Group> getGroups() {
		return out(BasicRelationships.HAS_GROUP).toList(Group.class);
	}

	public void addGroup(Group group) {
		linkOut(group, BasicRelationships.HAS_GROUP);
	}

}
