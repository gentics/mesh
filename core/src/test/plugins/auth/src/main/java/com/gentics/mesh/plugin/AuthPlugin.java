package com.gentics.mesh.server;

import java.util.List;
import java.util.stream.Collectors;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.auth.MappingResult;
import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class AuthPlugin extends AbstractPlugin implements AuthServicePlugin {
	public AuthPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public MappingResult mapToken(HttpServerRequest req, String userUuid, JsonObject token) {
		return new MappingResult()
			.setUser(extractUser(token))
			.setGroups(extractGroups(token))
			.setRoles(extractRoles(token));
	}

	private UserUpdateRequest extractUser(JsonObject token) {
		return new UserUpdateRequest()
			.setUsername(token.getString("preferred_username"))
			.setEmailAddress(token.getString("email"));
	}

	private List<GroupResponse> extractGroups(JsonObject token) {
		return token.getJsonArray("groups", new JsonArray())
			.stream()
			.map(JsonObject.class::cast)
			.map(group -> new GroupResponse()
				.setName(group.getString("name"))
				.setRoles(extractRolesFromGroup(group))
			)
			.collect(Collectors.toList());
	}

	private List<RoleReference> extractRolesFromGroup(JsonObject group) {
		return group.getJsonArray("roles")
			.stream()
			.map(name -> new RoleReference().setName((String) name))
			.collect(Collectors.toList());
	}

	private List<RoleResponse> extractRoles(JsonObject token) {
		return token.getJsonArray("groups", new JsonArray())
			.stream()
			.map(JsonObject.class::cast)
			.flatMap(group -> extractRolesFromGroup(group).stream())
			.map(roleReference -> new RoleResponse().setName(roleReference.getName()))
			.collect(Collectors.toList());
	}
}
