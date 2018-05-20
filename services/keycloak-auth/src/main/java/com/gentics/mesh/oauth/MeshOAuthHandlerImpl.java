package com.gentics.mesh.oauth;

import static com.gentics.mesh.core.rest.error.Errors.error;

import java.net.URL;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.auth.MeshOAuthHandler;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

@Singleton
public class MeshOAuthHandlerImpl implements MeshOAuthHandler {

	private OAuth2AuthHandler oauth2;
	private HttpClient client;

	@Inject
	public MeshOAuthHandlerImpl() {
		JsonObject config = loadRealmInfo();

		String host = config.getString("auth-server-host");
		int port = config.getInteger("auth-server-port");

		Vertx vertx = Mesh.vertx();
		client = vertx
			.createHttpClient(new HttpClientOptions().setDefaultHost(host).setDefaultPort(port));

		OAuth2Auth keyCloakAuth = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, config);
		// TODO configure callback url
		this.oauth2 = OAuth2AuthHandler.create(keyCloakAuth, "http://localhost:8080");
		// Don't setup the callback mechanism. This would create redirects in our rest api which we don't want. Instead we want to handle auth errors with http
		// error codes.
		// oauth2.setupCallback(router.get("/callback"));

	}

	/**
	 * Load the settings and enhance them with the public realm information from the auth server.
	 * 
	 * @return
	 */
	private JsonObject loadRealmInfo() {
		//TODO handle null
		JsonObject config = Mesh.mesh().getOptions().getAuthenticationOptions().getOauth2();
		String realmName = config.getString("realm");
		String url = config.getString("auth-server-url");
		try {
			URL authServerUrl = new URL(url);
			String authServerHost = authServerUrl.getHost();
			config.put("auth-server-host", authServerHost);
			int authServerPort = authServerUrl.getPort();
			config.put("auth-server-port", authServerPort);
			String authServerProtocol = authServerUrl.getProtocol();
			config.put("auth-server-protocol", authServerProtocol);

			Single<JsonObject> result = Single.create(sub -> {
				client.getNow("/auth/realms/" + realmName, rh -> {
					int code = rh.statusCode();
					if (code != 200) {
						sub.onError(new RuntimeException("Error while loading realm info. Got code {" + code + "}"));
					}
					rh.bodyHandler(bh -> {
						JsonObject json = bh.toJsonObject();
						sub.onSuccess(json);
					});
				});
			});
			JsonObject json = result.blockingGet();

			config.put("auth-server-url", authServerProtocol + "://" + authServerHost + ":" + authServerPort + "/auth");
			config.put("realm-public-key", json.getString("public_key"));
			return config;
		} catch (Exception e) {
			throw error(HttpResponseStatus.INTERNAL_SERVER_ERROR, "oauth_config_error", e);
		}

	}

	@Override
	public void handle(RoutingContext rc) {
		oauth2.handle(rc);
	}
}
