package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_USER;

import java.util.List;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.root.UserRoot;

public class UserRootImpl extends MeshVertexImpl implements UserRoot {

	public List<? extends User> getUsers() {
		return out(HAS_USER).toList(UserImpl.class);
	}

	public void addUser(UserImpl user) {
		linkOut(user, HAS_USER);
	}

	public void removeUser(UserImpl user) {
		unlinkOut(user, HAS_USER);
	}

	// TODO unique index

	public User create(String username) {
		UserImpl user = getGraph().addFramedVertex(UserImpl.class);
		user.setUsername(username);
		user.enable();
		addUser(user);
		return user;
	}

	@Override
	public UserRootImpl getImpl() {
		return this;
	}

}
