package com.gentics.mesh.rest;

import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.auth.TokenResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.rest.client.AbstractMeshRestHttpClient;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.HttpMethod;
import io.reactivex.Completable;
import io.reactivex.Single;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Authentication provider for the Mesh Rest Client.
 * TODO Replace this with generic way of adding request/response hooks
 * This way, the auth provider can add and set headers on its own on each request
 */
public class JWTAuthentication extends AbstractAuthenticationProvider {

	private String token;

	/**
	 * Flag is set, if the token is a login token (was set via {@link #login(AbstractMeshRestHttpClient)})
	 */
	private boolean loginToken = false;

	public JWTAuthentication() {
	}

	@Override
	public Completable addAuthenticationInformation(MeshRequest<?> request) {
		// TODO: request new Token when old one expires
		if (token != null) {
			request.setHeader("Cookie", "mesh.token=" + token);
			request.setHeader("Authorization", "Bearer " + token);
		}
		return Completable.complete();
	}

	@Override
	public Map<String, String> getHeaders() {
		if (token == null) {
			return Collections.emptyMap();
		}
		Map<String, String> headers = new HashMap<>(2);
		headers.put("Cookie", "mesh.token=" + token);
		headers.put("Authorization", "Bearer " + token);
		return headers;
	}

	@Override
	public Single<GenericMessageResponse> login(AbstractMeshRestHttpClient meshRestClient) {
		return Single.defer(() -> {
			LoginRequest loginRequest = new LoginRequest();
			if (!loginToken && StringUtils.isNotBlank(token)) {
				loginRequest.setApiKey(token);
			} else {
				loginRequest.setUsername(getUsername());
				loginRequest.setPassword(getPassword());
				loginRequest.setNewPassword(getNewPassword());
			}
			return meshRestClient.prepareRequest(HttpMethod.POST, "/auth/login", TokenResponse.class, loginRequest).toSingle();
		}).doOnSuccess(response -> {
			setLoginToken(response.getToken());
		}).map(ignore -> new GenericMessageResponse("OK"));
	}

	@Override
	public Single<GenericMessageResponse> logout(AbstractMeshRestHttpClient meshRestClient) {
		setToken(null);
		// No need call any endpoint in JWT
		return Single.just(new GenericMessageResponse("OK"));
	}

	/**
	 * Return the JWT token.
	 * 
	 * @return
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Set the JWT token.
	 * 
	 * @param token
	 * @return
	 */
	public JWTAuthentication setToken(String token) {
		this.token = token;
		this.loginToken = false;
		return this;
	}

	/**
	 * Set the JWT token as login token
	 * 
	 * @param token
	 * @return
	 */
	public JWTAuthentication setLoginToken(String token) {
		this.token = token;
		this.loginToken = true;
		return this;
	}

	/**
	 * Check whether the token is a login token
	 * @return true for a login token
	 */
	public boolean isLoginToken() {
		return loginToken;
	}
}
