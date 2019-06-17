package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;

/**
 * Aggregation node for users.
 */
public interface UserRoot extends RootVertex<User> {
	public static final String TYPE = "users";

	/**
	 * Create a new user with the given username and assign it to this aggregation node.
	 * 
	 * @param username
	 *            Username for the newly created user
	 * @param creator
	 *            User that is used to create creator and editor references
	 * @return
	 */
	default User create(String username, User creator) {
		return create(username, creator, null);
	}

	/**
	 * Create a new user with the given username and assign it to this aggregation node.
	 * 
	 * @param username
	 *            Username for the newly created user
	 * @param creator
	 *            User that is used to create creator and editor references
	 * @param uuid
	 *            Optional uuid
	 * @return
	 */
	User create(String username, User creator, String uuid);

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


}
