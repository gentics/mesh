package com.gentics.mesh.core.data.model.root.impl;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_USER;

import java.util.List;

import com.gentics.mesh.core.data.model.MeshUser;
import com.gentics.mesh.core.data.model.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.model.impl.MeshUserImpl;
import com.gentics.mesh.core.data.model.root.UserRoot;

public class UserRootImpl extends MeshVertexImpl implements UserRoot {

	public List<? extends MeshUser> getUsers() {
		return out(HAS_USER).toList(MeshUserImpl.class);
	}

	public void addUser(MeshUserImpl user) {
		linkOut(user, HAS_USER);
	}

	public void removeUser(MeshUserImpl user) {
		unlinkOut(user, HAS_USER);
	}

	// TODO unique index

	public MeshUser create(String username) {
		MeshUserImpl user = getGraph().addFramedVertex(MeshUserImpl.class);
		user.setUsername(username);
		addUser(user);
		return user;
	}

	@Override
	public UserRootImpl getImpl() {
		return this;
	}

}
