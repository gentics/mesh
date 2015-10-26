package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.query.QueryParameterProvider;

public interface UserClientMethods {

	/**
	 * Load a specific user by uuid.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	Future<UserResponse> findUserByUuid(String uuid, QueryParameterProvider... parameters);

	/**
	 * Load a specific user by username.
	 * 
	 * @param username
	 * @param parameters
	 * @return
	 */
	Future<UserResponse> findUserByUsername(String username, QueryParameterProvider... parameters);

	/**
	 * Load multiple users.
	 * 
	 * @param parameters
	 * @return
	 */
	Future<UserListResponse> findUsers(QueryParameterProvider... parameters);

	/**
	 * Create a new user.
	 * 
	 * @param request
	 * @param parameters
	 * @return
	 */
	Future<UserResponse> createUser(UserCreateRequest request, QueryParameterProvider... parameters);

	/**
	 * Update the user.
	 * 
	 * @param uuid
	 * @param request
	 * @param parameters
	 * @return
	 */
	Future<UserResponse> updateUser(String uuid, UserUpdateRequest request, QueryParameterProvider... parameters);

	/**
	 * Delete the user.
	 * 
	 * @param uuid
	 * @return
	 */
	Future<GenericMessageResponse> deleteUser(String uuid);

	/**
	 * Find users that were assigned to a specific group.
	 * 
	 * @param groupUuid
	 * @param parameters
	 * @return
	 */
	Future<UserListResponse> findUsersOfGroup(String groupUuid, QueryParameterProvider... parameters);

}
