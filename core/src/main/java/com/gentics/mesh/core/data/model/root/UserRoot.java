package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.User;

public class UserRoot extends MeshVertex {

	public List<? extends User> getUsers() {
		return out(BasicRelationships.HAS_USER).toList(User.class);
	}

	public void addUser(User user) {
		linkOut(user, BasicRelationships.HAS_USER);

	}

	public void removeUser(User user) {
		unlinkOut(user, BasicRelationships.HAS_USER);
	}

	// TODO unique index

}
