package com.gentics.mesh.plugin.auth;

import java.util.List;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.plugin.MeshPlugin;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * The authentication service plugin allows to hook into the authentication process.
 */
public interface AuthServicePlugin extends MeshPlugin {

	/**
	 * Check whether the token is accepted. Not accepting the token will result in a 401 error for the request.
	 * 
	 * @param httpServerRequest
	 * @param token
	 * @return True the token will be accepted and the request can pass. Otherwise the request will fail.
	 */
	default boolean acceptToken(HttpServerRequest httpServerRequest, JsonObject token) {
		return true;
	}

	/**
	 * Map the token information to mesh elements.
	 * @param rc 
	 * 
	 * @param token
	 * @return
	 */
	default MappingResult mapToken(RoutingContext rc, JsonObject token) {
		return null;
	}

	/**
	 * Check whether the user should be removed from the group.
	 * 
	 * @param groupName
	 * @param token
	 * @return true, the user will be removed from the group. Otherwise not.
	 */
	default boolean removeUserFromGroup(String groupName, JsonObject token) {
		return false;
	}

	/**
	 * Check whether the role with the given name should be removed from the group.
	 * 
	 * @param roleName
	 * @param groupName
	 * @param token
	 * @return true, the role will be removed from the group. Otherwise not
	 */
	default boolean removeRoleFromGroup(String roleName, String groupName, JsonObject token) {
		return false;
	}

}
