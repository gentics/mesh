package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.impl.UserRootImpl;

public interface UserRoot extends RootVertex<User> {

	User create(String username);

	UserRootImpl getImpl();

	MeshAuthUser findMeshAuthUserByUsername(String username);

	User findByUsername(String username);

	void removeUser(User user);

	void addUser(User user);

}
