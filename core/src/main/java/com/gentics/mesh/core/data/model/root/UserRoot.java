package com.gentics.mesh.core.data.model.root;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_USER;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;

public class UserRoot extends MeshVertex {

	public List<? extends MeshUser> getUsers() {
		return out(HAS_USER).toList(MeshUser.class);
	}

	public void addUser(MeshUser user) {
		linkOut(user, HAS_USER);
	}

	public void removeUser(MeshUser user) {
		unlinkOut(user, HAS_USER);
	}

	// TODO unique index

	public MeshUser create(String username) {
		MeshUser user = getGraph().addFramedVertex(MeshUser.class);
		user.setUsername(username);
		addUser(user);
		return user;
	}

}
