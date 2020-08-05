package com.gentics.mesh.core.data.root;

import java.util.Set;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.PermissionInfo;

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


	/**
	 * Set the plaintext password. Internally the password string will be hashed and the password hash will be set. This will also set
	 * {@link User#setForcedPasswordChange(boolean)} to false.
	 *
	 * @param user
	 * @param password
	 * @return Fluent API
	 */
	// TODO change this to an async call since hashing of the password is
	// blocking
	User setPassword(User user, String password);

	/**
	 * Return the permission info object for the given vertex.
	 *
	 * @param user
	 * @param vertex
	 * @return
	 */
	PermissionInfo getPermissionInfo(User user, MeshVertex vertex);

	/**
	 * Return a set of permissions which the user got for the given vertex.
	 *
	 * @param vertex
	 * @return
	 */
	Set<GraphPermission> getPermissions(User user, MeshVertex vertex);

	/**
	 * Check whether the user has the given permission on the given element.
	 *
	 * @param element
	 * @param permission
	 * @return
	 */
	boolean hasPermission(User user, MeshVertex element, GraphPermission permission);

	/**
	 * Check whether the user has the given permission on the element with the given id.
	 *
	 * @param elementId
	 * @param permission
	 * @return
	 */
	boolean hasPermissionForId(User user, Object elementId, GraphPermission permission);

}
