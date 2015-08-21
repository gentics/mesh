package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;

/**
 * Aggregation node for users.
 */

public interface UserRoot extends RootVertex<User> {
	public static final String TYPE = "users";

	/**
	 * Create a new user with the given username and assign it to this aggregation node and to the given group.
	 * 
	 * @param username
	 *            Username for the newly created user
	 * @param group
	 *            Group to which the user should be assigned to. The user will not be assigned to a group when this argument is null
	 * @param creator
	 *            User that is used to create creator and editor references
	 * @return
	 */
	User create(String username, Group group, User creator);

	MeshAuthUser findMeshAuthUserByUsername(String username);

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
