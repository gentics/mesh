package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;

public interface UserClientMethods {

	Future<UserResponse> findUserByUuid(String uuid);

	Future<UserResponse> findUserByUsername(String username);

	Future<UserListResponse> findUsers(PagingInfo pagingInfo);

	Future<UserResponse> createUser(UserCreateRequest userCreateRequest);

	Future<UserResponse> updateUser(String uuid, UserUpdateRequest userUpdateRequest);

	Future<GenericMessageResponse> deleteUser(String uuid);

	Future<UserListResponse> findUsersOfGroup(String groupUuid, PagingInfo pagingInfo);

}
