package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.user.UserResponse;

public interface UserRoot extends RootVertex<User, UserResponse> {

	User create(String username);

	MeshAuthUser findMeshAuthUserByUsername(String username);

	User findByUsername(String username);

	void addUser(User user);

	void removeUser(User user);

}
