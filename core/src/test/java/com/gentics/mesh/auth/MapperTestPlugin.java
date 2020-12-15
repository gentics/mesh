package com.gentics.mesh.auth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.auth.GroupFilter;
import com.gentics.mesh.plugin.auth.MappingResult;
import com.gentics.mesh.plugin.auth.RoleFilter;
import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Test plugin.
 */
public class MapperTestPlugin extends AbstractPlugin implements AuthServicePlugin {

	private static final Logger log = LoggerFactory.getLogger(MapperTestPlugin.class);

	public static boolean acceptToken;

	static {
		reset();
	}

	public static GroupFilter groupFilter;

	public static RoleFilter roleFilter;

	public static UserUpdateRequest userResult;

	public static List<RoleResponse> roleList;

	public static List<GroupResponse> groupList;

	public static Set<JsonObject> publicKeys = new HashSet<>();

	public static Function<JsonObject, Optional<String>> usernameExtractor;

	public MapperTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Set<JsonObject> getPublicKeys() {
		return publicKeys;
	}

	@Override
	public boolean acceptToken(HttpServerRequest httpServerRequest, JsonObject token) {
		return acceptToken;
	}

	@Override
	public Optional<String> extractUsername(JsonObject token) {
		if (usernameExtractor != null) {
			return usernameExtractor.apply(token);
		} else {
			return Optional.empty();
		}
	}

	@Override
	public MappingResult mapToken(HttpServerRequest req, String userUuid, JsonObject token) {
		MappingResult result = new MappingResult();

		log.info("Mapping groups in plugin");
		result.setGroups(groupList);

		log.info("Mapping role in plugin");
		result.setRoles(roleList);

		log.info("Mapping user in plugin");
		if (userResult != null) {
			String username = token.getString("preferred_username");
			userResult.setUsername(username);
		}
		result.setUser(userResult);
		result.setRoleFilter(roleFilter);
		result.setGroupFilter(groupFilter);
		return result;
	}

	private void printToken(JsonObject token) {
		String username = token.getString("preferred_username");
		System.out.println("Token for {" + username + "}");
		System.out.println(token.encodePrettily());
	}

	public static void reset() {
		MapperTestPlugin.acceptToken = true;
		MapperTestPlugin.publicKeys = new HashSet<>();

		roleFilter = (groupName, roleName) -> {
			log.info("Handling removal of role {" + roleName + "} from group {" + groupName + "}");
			return false;
		};

		groupFilter = (groupName) -> {
			log.info("Handling removal of user from group {" + groupName + "}");
			return false;
		};

		roleList = new ArrayList<>();
		roleList.add(new RoleResponse().setName("role1"));
		roleList.add(new RoleResponse().setName("role2"));
		roleList.add(new RoleResponse().setName("role3").setGroups(new GroupReference().setName("group1")));

		groupList = new ArrayList<>();
		groupList.add(new GroupResponse()
			.setName("group1"));

		groupList.add(new GroupResponse()
			.setName("group2")
			.setRoles(new RoleReference().setName("role1")));

		groupList.add(new GroupResponse()
			.setName("group3")
			.setRoles(new RoleReference().setName("role1"), new RoleReference().setName("role2")));

		UserUpdateRequest user = new UserUpdateRequest();
		user.setEmailAddress("mapped@email.tld");
		user.setFirstname("mappedFirstname");
		user.setLastname("mappedLastname");
		userResult = user;
	}

}
