package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Single;

public interface AuthClientMethods {

	/**
	 * Login the user using the credentials that have been set using {@link MeshRestClient#setLogin(String, String)}.
	 * 
	 * @return
	 */
	Single<GenericMessageResponse> login();

	/**
	 * Logout the user.
	 * 
	 * @return
	 */
	Single<GenericMessageResponse> logout();

	/**
	 * Return the currently active user's rest model data.
	 * 
	 * @param parameters
	 * @return
	 */
	MeshRequest<UserResponse> me(ParameterProvider... parameters);

}
