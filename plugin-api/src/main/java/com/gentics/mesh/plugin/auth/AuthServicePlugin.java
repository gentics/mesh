package com.gentics.mesh.plugin.auth;

import java.util.Collections;
import java.util.List;

import com.gentics.mesh.plugin.MeshPlugin;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

/**
 * The authentication service plugin allows to hook into the authentication process.
 */
public interface AuthServicePlugin extends MeshPlugin {

	/**
	 * Return a list of X509 formatted public keys which will be used to validate JWT's that are included in API requests. You can use this method to provide
	 * public keys from your identity provider server (e.g. keycloak). Adding the needed public key will enable Gentics Mesh to accept keys which have been
	 * issued by those servers. Gentics Mesh will be able to validate the tokens in terms of OAuth2 code flow (acting as resource server).
	 * 
	 * @return List of public keys
	 */
	default List<String> getPublicKeys() {
		return Collections.emptyList();
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
