package com.gentics.mesh.auth;

import static com.gentics.mesh.core.rest.error.Errors.error;

import java.io.IOException;
import java.net.URL;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;

public final class KeycloakUtils {

	private static final Logger log = LoggerFactory.getLogger(KeycloakUtils.class);

	private KeycloakUtils() {
	}

	public static String loadPublicKey(String realmName, String authServerUrl) {
		try {
			URL parsedAuthServerUrl = new URL(authServerUrl);
			String authServerHost = parsedAuthServerUrl.getHost();
			int authServerPort = parsedAuthServerUrl.getPort();
			String authServerProtocol = parsedAuthServerUrl.getProtocol();
			JsonObject json = fetchPublicRealmInfo(authServerProtocol, authServerHost, authServerPort, realmName);
			return json.getString("public_key");
		} catch (Exception e) {
			throw error(HttpResponseStatus.INTERNAL_SERVER_ERROR, "oauth_config_error", e);
		}

	}

	protected static JsonObject fetchPublicRealmInfo(String protocol, String host, int port, String realmName) throws IOException {
		Builder builder = new OkHttpClient.Builder();
		OkHttpClient client = builder.build();

		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.url(protocol + "://" + host + ":" + port + "" + "/auth/realms/" + realmName)
			.build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				log.error(response.body().toString());

				throw new RuntimeException("Error while loading realm info. Got code {" + response.code() + "}");
			}
			return new JsonObject(response.body().string());
		}
	}

}
