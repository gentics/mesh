package com.gentics.mesh.rest;

import org.apache.commons.codec.binary.Base64;

import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;
import rx.Observable;

public class BasicAuthentication extends AbstractAuthentication {

	private String authHeader;
	private Cookie sessionID;
	
	public BasicAuthentication() {
	}
	
	public BasicAuthentication(RoutingContext context) {
		super();
		this.authHeader = context.request().getHeader("Authorization");
		this.sessionID = context.getCookie(MeshOptions.MESH_SESSION_KEY);
	}

	@Override
	public Observable<Void> addAuthenticationInformation(HttpClientRequest request) {
		if (authHeader != null) {
			request.headers().add("Authorization", authHeader);
		} else if (sessionID != null) {
			request.putHeader("Cookie", sessionID.encode());
		} else if (getUsername() != null && getPassword() != null) {
			String authStringEnc = getUsername() + ":" + getPassword();
			String authEnc = new String(Base64.encodeBase64(authStringEnc.getBytes()));
			authHeader = "Basic " + authEnc;
	
			request.headers().add("Authorization", authHeader);
		}
		return Observable.just(null);
	}

	@Override
	public Observable<GenericMessageResponse> login(HttpClient client) {
		return Observable.create(sub -> {
			LoginRequest loginRequest = new LoginRequest();
			loginRequest.setUsername(getUsername());
			loginRequest.setPassword(getPassword());
			
			MeshRestRequestUtil.handleRequest(HttpMethod.POST, "/auth/login", GenericMessageResponse.class, loginRequest, client, null, null).setHandler(rh -> {
				if (rh.failed()) {
					sub.onError(rh.cause());
				} else {
					sub.onNext(rh.result());
					sub.onCompleted();
				}
			});
		});
	}

	@Override
	public Observable<GenericMessageResponse> logout(HttpClient client) {
		return Observable.create(sub -> {
			MeshRestRequestUtil.handleRequest(HttpMethod.GET, "/auth/logout", GenericMessageResponse.class, client, null, null).setHandler(rh -> {
				if (rh.failed()) {
					sub.onError(rh.cause());
				} else {
					sub.onNext(rh.result());
					sub.onCompleted();
				}
			});
		});
		
	}
}
