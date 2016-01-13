package com.gentics.mesh.rest;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import rx.Observable;

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
	public Observable<Void> addAuthenticationInformation(HttpClientRequest request);

	/**
	 * Logs the user in with the credentials that were set with {@link setLogin}
	 * @return A future that completes when the login has completed.
	 */
	public Observable<GenericMessageResponse> login(HttpClient client);

	/**
	 * Logs out the user.
	 * @return A future that completes when the logout has completed.
	 */
	public Observable<GenericMessageResponse> logout(HttpClient client);
}
