package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.QueryParameterProvider;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;

public interface UserClientMethods {

	Future<UserResponse> findUserByUuid(String uuid, QueryParameterProvider... parameters);

	Future<UserResponse> findUserByUsername(String username, QueryParameterProvider... parameters);

	Future<UserListResponse> findUsers(QueryParameterProvider... parameters);

	Future<UserResponse> createUser(UserCreateRequest userCreateRequest, QueryParameterProvider... parameters);

	Future<UserResponse> updateUser(String uuid, UserUpdateRequest userUpdateRequest, QueryParameterProvider... parameters);

	Future<GenericMessageResponse> deleteUser(String uuid);

	Future<UserListResponse> findUsersOfGroup(String groupUuid, QueryParameterProvider... parameters);

}
