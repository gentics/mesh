package com.gentics.mesh.rest;

import org.apache.commons.codec.binary.Base64;

import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

public class BasicAuthentification extends AbstractAuthentification {

	@Override
	public Observable<Void> addAuthentificationInformation(HttpClientRequest request) {
		if (getUsername() != null && getPassword() != null) {
			String authStringEnc = getUsername() + ":" + getPassword();
			String authEnc = new String(Base64.encodeBase64(authStringEnc.getBytes()));
	
			request.headers().add("Authorization", "Basic " + authEnc);
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
