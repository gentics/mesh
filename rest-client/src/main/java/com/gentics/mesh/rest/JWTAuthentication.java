package com.gentics.mesh.rest;

import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.auth.TokenResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.rest.client.MeshRestRequestUtil;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import rx.Completable;
import rx.Single;

public class JWTAuthentication extends AbstractAuthenticationProvider {

	private String token;
	private Single<GenericMessageResponse> loginRequest;

	public JWTAuthentication() {
	}

	public JWTAuthentication(RoutingContext context) {
		super();
	}

	@Override
	public Completable addAuthenticationInformation(HttpClientRequest request) {
		//TODO: request new Token when old one expires
		request.headers().add("Authorization", "Bearer " + token);
		return Completable.complete();
	}

	@Override
	public Single<GenericMessageResponse> login(HttpClient client) {
		this.loginRequest = Single.create(sub -> {
			LoginRequest loginRequest = new LoginRequest();
			loginRequest.setUsername(getUsername());
			loginRequest.setPassword(getPassword());

			MeshRestRequestUtil.prepareRequest(HttpMethod.POST, "/auth/login", TokenResponse.class, loginRequest, client, null).invoke()
					.setHandler(rh -> {
						if (rh.failed()) {
							sub.onError(rh.cause());
						} else {
							token = rh.result().getToken();
							sub.onSuccess(new GenericMessageResponse("OK"));
						}
					});
		});
		return this.loginRequest;
	}

	@Override
	public Single<GenericMessageResponse> logout(HttpClient client) {
		token = null;
		loginRequest = null;
		//No need call any endpoint in JWT
		return Single.just(new GenericMessageResponse("OK"));
	}

}
