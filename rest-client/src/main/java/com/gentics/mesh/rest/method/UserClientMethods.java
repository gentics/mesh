package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.QueryParameterProvider;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;

public interface UserClientMethods {

	Future<UserResponse> findUserByUuid(String uuid);

	Future<UserResponse> findUserByUsername(String username);

	Future<UserListResponse> findUsers(QueryParameterProvider... parameters);

	Future<UserResponse> createUser(UserCreateRequest userCreateRequest);

	Future<UserResponse> updateUser(String uuid, UserUpdateRequest userUpdateRequest);

	Future<GenericMessageResponse> deleteUser(String uuid);

	Future<UserListResponse> findUsersOfGroup(String groupUuid, QueryParameterProvider... parameters);

}
