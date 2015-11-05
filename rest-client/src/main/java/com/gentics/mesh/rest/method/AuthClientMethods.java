package com.gentics.mesh.rest.method;

import io.vertx.core.Future;
import rx.Observable;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.MeshRestClient;

public interface AuthClientMethods {

	/**
	 * Login the user using the credentials that have been set using {@link MeshRestClient#setLogin(String, String)}.
	 * 
	 * @return
	 */
	Observable<GenericMessageResponse> login();

	/**
	 * Logout the user.
	 * 
	 * @return
	 */
	Observable<GenericMessageResponse> logout();

	/**
	 * Return the currently active user's rest model data.
	 * 
	 * @return
	 */
	Future<UserResponse> me();

	Future<GenericMessageResponse> permissions(String roleUuid, String objectUuid, Permission permission, boolean recursive);

}
