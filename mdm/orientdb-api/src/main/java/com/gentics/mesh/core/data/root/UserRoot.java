package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.user.UserResponse;

/**
 * Aggregation node for users.
 */
public interface UserRoot extends RootVertex<User>, TransformableElementRoot<User, UserResponse> {
	public static final String TYPE = "users";

	/**
	 * Find the user with the given username.
	 * 
	 * @param username
	 * @return
	 */
	User findByUsername(String username);

	/**
	 * Add the user to the aggregation vertex.
	 * 
	 * @param user
	 */
	void addUser(User user);

	/**
	 * Remove the user from the aggregation vertex.
	 * 
	 * @param user
	 */
	void removeUser(User user);

	/**
	 * Find the mesh auth user with the given username.
	 * 
	 * @param username
	 * @return
	 */
	MeshAuthUser findMeshAuthUserByUsername(String username);

	/**
	 * Find the mesh auth user with the given UUID.
	 * 
	 * @param userUuid
	 * @return
	 */
	MeshAuthUser findMeshAuthUserByUuid(String userUuid);

	User create();
}
