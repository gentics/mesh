package com.gentics.mesh.auth.util;

import static com.gentics.mesh.core.rest.error.Errors.error;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Utilities for keycloak SSO.
 */
public final class KeycloakUtils {

	private static final Logger log = LoggerFactory.getLogger(KeycloakUtils.class);

	private KeycloakUtils() {
	}

	/**
	 * Load the public key from the realm.
	 * 
	 * @param realmName
	 * @param authServerUrl
	 * @return
	 */
	public static String loadPublicKey(String realmName, String authServerUrl) {
		try {
			URL parsedAuthServerUrl = new URL(authServerUrl);
			String authServerHost = parsedAuthServerUrl.getHost();
			int authServerPort = parsedAuthServerUrl.getPort();
			String authServerProtocol = parsedAuthServerUrl.getProtocol();
			JsonObject json = fetchPublicRealmInfo(authServerProtocol, authServerHost, authServerPort, realmName);
			return json.getString("public_key");
		} catch (Exception e) {
			// TODO i18n
			throw error(HttpResponseStatus.INTERNAL_SERVER_ERROR, "oauth_config_error", e);
		}
	}

	/**
	 * Load the JWKs from the realm.
	 * 
	 * @param protocol
	 * @param host
	 * @param port
	 * @param realmName
	 * @return
	 * @throws IOException
	 */
	public static Set<JsonObject> loadJWKs(String protocol, String host, int port, String realmName) throws IOException {
		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.url(protocol + "://" + host + ":" + port + "" + "/auth/realms/" + realmName + "/protocol/openid-connect/certs")
			.build();

		try (Response response = httpClient().newCall(request).execute()) {
			if (!response.isSuccessful()) {
				log.error(response.body().toString());
				throw new RuntimeException("Error while loading certs. Got code {" + response.code() + "}");
			}
			JsonObject json = new JsonObject(response.body().string());
			JsonArray keys = json.getJsonArray("keys");
			Set<JsonObject> jwks = new HashSet<>();
			for (int i = 0; i < keys.size(); i++) {
				JsonObject key = keys.getJsonObject(i);
				jwks.add(key);
			}
			return jwks;
		}

	}

	public static JsonObject fetchPublicRealmInfo(String protocol, String host, int port, String realmName) throws IOException {

		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.url(protocol + "://" + host + ":" + port + "" + "/auth/realms/" + realmName)
			.build();

		try (Response response = httpClient().newCall(request).execute()) {
			if (!response.isSuccessful()) {
				log.error(response.body().toString());

				throw new RuntimeException("Error while loading realm info. Got code {" + response.code() + "}");
			}
			return new JsonObject(response.body().string());
		}
	}

	/**
	 * Invoke a login request.
	 * 
	 * @param protocol
	 * @param host
	 * @param port
	 * @param realmName
	 * @param clientId
	 * @param username
	 * @param password
	 * @param secret
	 * @return
	 * @throws IOException
	 */
	public static JsonObject loginKeycloak(String protocol, String host, int port, String realmName, String clientId, String username,
		String password, String secret) throws IOException {

		StringBuilder content = new StringBuilder();
		content.append("client_id=" + clientId + "&");
		content.append("username=" + username + "&");
		content.append("password=" + password + "&");
		content.append("grant_type=password&");
		content.append("client_secret=" + secret);
		RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), content.toString());
		Request request = new Request.Builder()
			.post(body)
			.url(protocol + "://" + host + ":" + port + "/auth/realms/" + realmName + "/protocol/openid-connect/token")
			.build();

		Response response = httpClient().newCall(request).execute();
		return new JsonObject(response.body().string());
	}

	private static OkHttpClient httpClient() {
		Builder builder = new OkHttpClient.Builder();
		OkHttpClient client = builder.build();
		return client;
	}

}
