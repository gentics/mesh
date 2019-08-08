package com.gentics.mesh.auth;

import java.util.ArrayList;
import java.util.List;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.auth.MappingResult;
import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class AuthServiceTestPlugin extends AbstractPlugin implements AuthServicePlugin {

	public AuthServiceTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public MappingResult mapToken(HttpServerRequest req, String userUuid, JsonObject token) {
		MappingResult result = new MappingResult();
		List<GroupResponse> groups = new ArrayList<>();
		groups.add(new GroupResponse().setName("group1"));
		groups.add(new GroupResponse().setName("group2"));
		result.setGroups(groups);

		List<RoleResponse> roles = new ArrayList<>();
		roles.add(new RoleResponse().setName("role1"));
		roles.add(new RoleResponse().setName("role2"));
		result.setRoles(roles);
		return result;
	}

}
