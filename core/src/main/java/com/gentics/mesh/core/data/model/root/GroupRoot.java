package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Group;

public class GroupRoot extends MeshVertex {

	// TODO unique node

	public List<? extends Group> getGroups() {
		return out(MeshRelationships.HAS_GROUP).toList(Group.class);
	}

	public void addGroup(Group group) {
		linkOut(group, MeshRelationships.HAS_GROUP);
	}

}
