package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;

public class UserRoot extends MeshVertex {

	public List<? extends MeshUser> getUsers() {
		return out(MeshRelationships.HAS_USER).toList(MeshUser.class);
	}

	public void addUser(MeshUser user) {
		linkOut(user, MeshRelationships.HAS_USER);

	}

	public void removeUser(MeshUser user) {
		unlinkOut(user, MeshRelationships.HAS_USER);
	}

	// TODO unique index

}
