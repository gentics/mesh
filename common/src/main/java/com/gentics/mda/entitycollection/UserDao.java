package com.gentics.mda.entitycollection;

import com.gentics.mda.entity.AUser;
import com.gentics.mesh.core.data.MeshAuthUser;

public interface UserDao extends MeshDao<AUser> {
	/**
	 * Create a new user with the given username and assign it to this aggregation node.
	 *
	 * @param username
	 *            Username for the newly created user
	 * @param creator
	 *            User that is used to create creator and editor references
	 * @return
	 */
	default AUser create(String username, AUser creator) {
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
	AUser create(String username, AUser creator, String uuid);

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
	AUser findByUsername(String username);

	/**
	 * Remove the user from the aggregation vertex.
	 *
	 * @param user
	 */
	void removeUser(AUser user);
}
