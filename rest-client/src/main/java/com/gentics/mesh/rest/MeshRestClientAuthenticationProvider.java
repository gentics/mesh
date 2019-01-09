package com.gentics.mesh.rest;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;

import com.gentics.mesh.rest.client.MeshRestClient;
import io.vertx.core.http.HttpClientRequest;
import io.reactivex.Completable;
import io.reactivex.Single;

import java.util.Map;

public interface MeshRestClientAuthenticationProvider {
	
	/**
	 * Sets the credentials used to authenticate the user
	 * @param username
	 * @param password
	 */
	public void setLogin(String username, String password);
	
	/**
	 * Modifies the provided request by adding authentication information.
	 * @param request
	 * @return 
	 */
	public Completable addAuthenticationInformation(HttpClientRequest request);

	/**
	 * Logs the user in with the credentials that were set with {@link setLogin}
	 * @return A future that completes when the login has completed.
	 */
	public Single<GenericMessageResponse> login(MeshRestClient client);

	/**
	 * Logs out the user.
	 * @return A future that completes when the logout has completed.
	 */
	public Single<GenericMessageResponse> logout(MeshRestClient client);

	/**
	 * Gets authentication information as http headers.
	 * @return A map containing the headers.
	 */
	Map<String, String> getHeaders();
}
