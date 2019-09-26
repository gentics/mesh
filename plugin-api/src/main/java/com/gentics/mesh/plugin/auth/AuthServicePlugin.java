package com.gentics.mesh.plugin.auth;

import java.util.Collections;
import java.util.Set;

import com.gentics.mesh.plugin.MeshPlugin;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

/**
 * The authentication service plugin allows to hook into the authentication process.
 */
public interface AuthServicePlugin extends MeshPlugin {

	/**
	 * Return a list JWK public keys which will be used to validate JWT's that are included in API requests. You can use this method to provide public keys from
	 * your identity provider server (e.g. keycloak). Adding the needed public key will enable Gentics Mesh to accept keys which have been issued by those
	 * servers. Gentics Mesh will be able to validate the tokens in terms of OAuth2 code flow (acting as resource server).
	 * 
	 * @return Set of JWK's
	 */
	default Set<JsonObject> getPublicKeys() {
		return Collections.emptySet();
	}

	/**
	 * Check whether the token is accepted. Not accepting the token will result in a 401 error for the request.
	 * 
	 * @param httpServerRequest
	 * @param token
	 *            Authentication token that has been passed to Gentics Mesh
	 * @return True the token will be accepted and the request can pass. Otherwise the request will fail.
	 */
	default boolean acceptToken(HttpServerRequest httpServerRequest, JsonObject token) {
		return true;
	}

	/**
	 * Process the token before it is getting mapped to a mesh user. This step can be used to augment the token with additional properties.
	 * 
	 * @param token
	 * @return
	 */
	default JsonObject preProcessToken(JsonObject token) {
		return token;
	}

	/**
	 * Map the token information to mesh elements. You can use this method to extract information from the token and sync roles, groups in Gentics Mesh.
	 * 
	 * @param req
	 *            Http Sever request that needs authentication
	 * @param userUuid
	 *            Uuid of the user. This will only be set if the user has already been created before.
	 * @param token
	 *            Authentication token that has been passed to Gentics Mesh
	 * @return Mapping result which will be used to setup the roles, groups and user
	 */
	default MappingResult mapToken(HttpServerRequest req, String userUuid, JsonObject token) {
		return null;
	}



}
