package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_USER;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.etc.MeshSpringConfiguration;

public class UserRootImpl extends AbstractRootVertex<User> implements UserRoot {

	@Override
	protected Class<? extends User> getPersistanceClass() {
		return UserImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_USER;
	}

	@Override
	public void addUser(User user) {
		addItem(user);
	}

	@Override
	public void removeUser(User user) {
		removeItem(user);
	}

	// TODO unique index

	@Override
	public User create(String username) {
		UserImpl user = getGraph().addFramedVertex(UserImpl.class);
		user.setUsername(username);
		user.enable();
		addItem(user);
		return user;
	}

	@Override
	public User findByUsername(String username) {
		return out(HAS_USER).has(UserImpl.class).has(UserImpl.USERNAME_KEY, username).nextOrDefaultExplicit(UserImpl.class, null);
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUsername(String username) {
		return out(HAS_USER).has(UserImpl.class).has(UserImpl.USERNAME_KEY, username).nextOrDefaultExplicit(MeshAuthUserImpl.class, null);
	}

}
