package com.gentics.mesh.core.data.model.root;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_GROUP;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.tinkerpop.Group;

public class GroupRoot extends MeshVertex {

	// TODO unique node

	public List<? extends Group> getGroups() {
		return out(HAS_GROUP).toList(Group.class);
	}

	public void addGroup(Group group) {
		linkOut(group, HAS_GROUP);
	}

}
