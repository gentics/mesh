package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserAPITokenResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserResetTokenResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

public interface UserClientMethods {

	/**
	 * Load a specific user by uuid.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<UserResponse> findUserByUuid(String uuid, ParameterProvider... parameters);

	/**
	 * Load multiple users.
	 * 
	 * @param parameters
	 * @return
	 */
	MeshRequest<UserListResponse> findUsers(ParameterProvider... parameters);

	/**
	 * Create a new user.
	 * 
	 * @param request
	 * @param parameters
	 * @return
	 */
	MeshRequest<UserResponse> createUser(UserCreateRequest request, ParameterProvider... parameters);

	/**
	 * Create a new user using the provided uuid.
	 * 
	 * @param uuid
	 * @param request
	 * @param parameters
	 * @return
	 */
	MeshRequest<UserResponse> createUser(String uuid, UserCreateRequest request, ParameterProvider... parameters);

	/**
	 * Update the user.
	 * 
	 * @param uuid
	 *            User uuid
	 * @param request
	 * @param parameters
	 * @return
	 */
	MeshRequest<UserResponse> updateUser(String uuid, UserUpdateRequest request, ParameterProvider... parameters);

	/**
	 * Delete the user.
	 *
	 * @param uuid User uuid
	 * @return
	 */
	MeshRequest<EmptyResponse> deleteUser(String uuid);

	/**
	 * Find users that were assigned to a specific group.
	 *
	 * @param groupUuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<UserListResponse> findUsersOfGroup(String groupUuid, ParameterProvider... parameters);

	/**
	 * Read the user permissions for the given path.
	 * 
	 * @param uuid
	 *            User uuid
	 * @param pathToElement
	 *            Path to the element
	 * @return
	 */
	MeshRequest<UserPermissionResponse> readUserPermissions(String uuid, String pathToElement);

	/**
	 * Fetch a new user token for the user with the given uuid. Note that any previously fetched token for that particular user will be invalidated by this
	 * action.
	 * 
	 * @param userUuid
	 *            User uuid
	 * @return
	 */
	MeshRequest<UserResetTokenResponse> getUserResetToken(String userUuid);

	/**
	 * Generate a new API token for the user. The token is valid until a new token is generated. Generating a new token will invalidate the previously generated
	 * one.
	 * 
	 * @param userUuid
	 *            User uuid
	 * @return
	 */
	MeshRequest<UserAPITokenResponse> issueAPIToken(String userUuid);

	/**
	 * Invalidate the currently active API token.
	 * 
	 * @param userUuid
	 * @return
	 */
	MeshRequest<GenericMessageResponse> invalidateAPIToken(String userUuid);
}
