package com.gentics.mesh.rest;

import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.auth.TokenResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public class JWTAuthentification extends AbstractAuthentification{

	private String token;
	private Observable<GenericMessageResponse> loginRequest;
	
//	public JWTAuthentification(HttpActionContext context) {
//		
//	}
	
	public JWTAuthentification() {
	}

	@Override
	public Observable<Void> addAuthentificationInformation(HttpClientRequest request) {
		//TODO: request new Token when old one expires
		
		if (token != null) {
			request.headers().add("Authorization", "Bearer " + token);
			return Observable.just(null);
		} else {
			return loginRequest.map(x -> {
				request.headers().add("Authorization", "Bearer " + token);
				return null;
			});
		}
	}

	@Override
	public Observable<GenericMessageResponse> login(HttpClient client) {
		this.loginRequest = Observable.create(sub -> {
			LoginRequest loginRequest = new LoginRequest();
			loginRequest.setUsername(getUsername());
			loginRequest.setPassword(getPassword());
			
			MeshRestRequestUtil.handleRequest(HttpMethod.POST, "/auth/login", TokenResponse.class, loginRequest, client, null, null).setHandler(rh -> {
				if (rh.failed()) {
					sub.onError(rh.cause());
				} else {
					token = rh.result().getToken();
					sub.onNext(new GenericMessageResponse("OK"));
					sub.onCompleted();
				}
			});
		});
		this.loginRequest = this.loginRequest.cache(1);
		return this.loginRequest;
	}

	@Override
	public Observable<GenericMessageResponse> logout(HttpClient client) {
		//No need to logout in JWT
		return Observable.just(new GenericMessageResponse("OK"));
	}

}
