package com.gentics.mesh.dagger;

import static com.gentics.mesh.core.rest.error.Errors.error;

import java.net.URL;

import javax.inject.Named;
import javax.inject.Singleton;

import com.gentics.mesh.auth.OAuth2AuthCookieHandler;
import com.gentics.mesh.auth.OAuth2AuthCookieHandlerImpl;
import com.gentics.mesh.etc.config.MeshOptions;

import dagger.Module;
import dagger.Provides;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;

@Module
public interface OAuth2Module {

	@Provides
	public static OAuth2AuthCookieHandler cookieHandler(OAuth2Auth auth) {
		return new OAuth2AuthCookieHandlerImpl(auth);
	}

	@Provides
	public static OAuth2Auth oauth2(Vertx vertx, @Named("oauth") JsonObject config) {
		return KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, config);
	}

	/**
	 * Load the settings and enhance them with the public realm information from the auth server.
	 * 
	 * @return
	 */
	@Singleton
	@Provides
	@Named("oauth")
	public static JsonObject loadRealmInfo(Vertx vertx, MeshOptions options) {
		// TODO handle null

		JsonObject config = options.getAuthenticationOptions().getOauth2().getConfig();
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
				// TODO configure ssl flag
				HttpClient client = vertx
					.createHttpClient(new HttpClientOptions().setDefaultHost(authServerHost).setDefaultPort(authServerPort));
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
				client.close();
			});
			JsonObject json = result.blockingGet();

			config.put("auth-server-url", authServerProtocol + "://" + authServerHost + ":" + authServerPort + "/auth");
			config.put("realm-public-key", json.getString("public_key"));
			return config;
		} catch (Exception e) {
			throw error(HttpResponseStatus.INTERNAL_SERVER_ERROR, "oauth_config_error", e);
		}

	}
}
