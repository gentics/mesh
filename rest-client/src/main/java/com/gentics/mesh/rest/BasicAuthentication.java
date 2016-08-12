package com.gentics.mesh.rest;

import org.apache.commons.codec.binary.Base64;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshRestClientHttpException;
import com.gentics.mesh.rest.client.MeshRestRequestUtil;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import rx.Completable;
import rx.Single;

public class BasicAuthentication extends AbstractAuthenticationProvider {

	private String authHeader;
	private String cookies;

	private static final Logger log = LoggerFactory.getLogger(BasicAuthentication.class);

	public BasicAuthentication() {
	}

	public BasicAuthentication(RoutingContext context) {
		super();
		this.authHeader = context.request().getHeader("Authorization");
		this.cookies = context.getCookie(MeshOptions.MESH_SESSION_KEY).encode();
	}

	@Override
	public Completable addAuthenticationInformation(HttpClientRequest request) {
		if (cookies != null) {
			request.putHeader("Cookie", cookies);
		} else if (authHeader != null) {
			request.headers().add("Authorization", authHeader);
		} else if (getUsername() != null && getPassword() != null) {
			String authStringEnc = getUsername() + ":" + getPassword();
			String authEnc = new String(Base64.encodeBase64(authStringEnc.getBytes()));
			authHeader = "Basic " + authEnc;

			request.headers().add("Authorization", authHeader);
		}
		return Completable.complete();
	}

	@Override
	public Single<GenericMessageResponse> login(HttpClient client) {
		//This calls the login endpoint and saves the session cookie which is used for future calls.
		//That way, mesh doesn't have to check the password every time (which saves a lot of performance)

		return Single.create(sub -> {
			JsonObject json = new JsonObject().put("username", this.getUsername()).put("password", this.getPassword());

			HttpClientRequest req = client.post(MeshRestRequestUtil.BASEURI + "/auth/login", resp -> {
				if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
					cookies = resp.headers().get("Set-Cookie");
					sub.onSuccess(new GenericMessageResponse("OK"));
				} else {
					resp.bodyHandler(buffer -> {
						String body = buffer.toString();
						try {
							GenericMessageResponse msgResp = JsonUtil.readValue(body, GenericMessageResponse.class);
							MeshRestClientHttpException error = new MeshRestClientHttpException(resp.statusCode(), resp.statusMessage(), msgResp);
							sub.onError(error);
						} catch (Exception e) {
							log.error("Could not parse JSON. This should not have happened. Response JSON:\n" + body);
							sub.onError(e);
						}
					});
				}
			});
			req.putHeader("Accept", "application/json");
			req.putHeader("Content-Type", "application/json");
			req.end(json.encode());
		});
	}

	@Override
	public Single<GenericMessageResponse> logout(HttpClient client) {
		return Single.create(sub -> {
			MeshRestRequestUtil.handleRequest(HttpMethod.GET, "/auth/logout", GenericMessageResponse.class, client, this).invoke().setHandler(rh -> {
				if (rh.failed()) {
					sub.onError(rh.cause());
				} else {
					sub.onSuccess(rh.result());
				}
			});
		});

	}
}
