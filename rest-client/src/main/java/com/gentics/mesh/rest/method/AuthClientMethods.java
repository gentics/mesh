package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.user.UserResponse;

public interface AuthClientMethods {

	Future<GenericMessageResponse> login();

	Future<GenericMessageResponse> logout();

	Future<UserResponse> me();

	Future<GenericMessageResponse> permissions(String roleUuid, String objectUuid, Permission permission, boolean recursive);


}
