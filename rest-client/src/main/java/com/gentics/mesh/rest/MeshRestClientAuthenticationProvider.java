package com.gentics.mesh.rest;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.rest.client.AbstractMeshRestHttpClient;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Single;

import java.util.Map;

/**
 * Definition for the authentication provider for the {@link MeshRestClient}. It defines methods to handle login / logout and parsing of auth information.
 */
public interface MeshRestClientAuthenticationProvider {

	/**
	 * Sets the credentials used to authenticate the user
	 * @param username
	 * @param password
	 */
	void setLogin(String username, String password);

	/**
	 * Sets the credentials used to authenticate the user
	 * Also sets a new password for the user when {@link #login(AbstractMeshRestHttpClient)} is called
	 * @param username
	 * @param password
	 */
	void setLogin(String username, String password, String newPassword);

	/**
	 * Modifies the provided request by adding authentication information.
	 * @param request
	 * @return
	 */
	Completable addAuthenticationInformation(MeshRequest<?> request);

	/**
	 * Logs the user in with the credentials that were set with {@link #setLogin(String, String)}
	 * @return A future that completes when the login has completed.
	 */
	Single<GenericMessageResponse> login(AbstractMeshRestHttpClient client);

	/**
	 * Logs out the user.
	 * @return A future that completes when the logout has completed.
	 */
	Single<GenericMessageResponse> logout(AbstractMeshRestHttpClient client);

	/**
	 * Gets authentication information as http headers.
	 * @return A map containing the headers.
	 */
	Map<String, String> getHeaders();
}
