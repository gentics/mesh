package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;

/**
 * Aggregation node for users.
 */
public interface UserRoot extends RootVertex<User> {

	User create(String username, Group group, User creator);

	MeshAuthUser findMeshAuthUserByUsername(String username);

	User findByUsername(String username);

	void addUser(User user);

	void removeUser(User user);

}
