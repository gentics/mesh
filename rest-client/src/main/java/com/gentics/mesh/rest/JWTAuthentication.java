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

/**
 * Authentication provider for the Mesh Rest Client.
 * TODO Replace this with generic way of adding request/response hooks
 * This way, the auth provider can add and set headers on its own on each request
 */
public class JWTAuthentication extends AbstractAuthenticationProvider {

	private String token;

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
			loginRequest.setUsername(getUsername());
			loginRequest.setPassword(getPassword());
			loginRequest.setNewPassword(getNewPassword());

			return meshRestClient.prepareRequest(HttpMethod.POST, "/auth/login", TokenResponse.class, loginRequest).toSingle();
		}).doOnSuccess(response -> token = response.getToken())
			.map(ignore -> new GenericMessageResponse("OK"));
	}

	@Override
	public Single<GenericMessageResponse> logout(AbstractMeshRestHttpClient meshRestClient) {
		token = null;
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
		return this;
	}

}
