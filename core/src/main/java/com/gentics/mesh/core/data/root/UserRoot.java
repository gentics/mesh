package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;

public interface UserRoot extends RootVertex<User> {

	User create(String username);

	MeshAuthUser findMeshAuthUserByUsername(String username);

	User findByUsername(String username);

	void addUser(User user);

	void removeUser(User user);

}
