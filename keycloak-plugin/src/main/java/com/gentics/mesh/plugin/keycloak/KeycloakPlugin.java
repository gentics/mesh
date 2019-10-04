package com.gentics.mesh.plugin.keycloak;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.pf4j.PluginWrapper;

import com.gentics.mesh.auth.util.KeycloakUtils;
import com.gentics.mesh.plugin.AbstractCommercialPlugin;
import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.auth.MappingResult;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.keycloak.config.KeycloakPluginConfig;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.reactivex.Completable;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class KeycloakPlugin extends AbstractCommercialPlugin<KeycloakPluginConfig> implements AuthServicePlugin {

	private static final String DEFAULT_SCRIPT_NAME = "defaultMapperScript.groovy";
	private static final String MAPPER_SCRIPT_NAME = "mapperScript.groovy";
	private static final Logger log = LoggerFactory.getLogger(KeycloakPlugin.class);
	private Set<JsonObject> publicKeys = Collections.emptySet();
	private String mapperScript;

	public KeycloakPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Set<JsonObject> getPublicKeys() {
		return publicKeys;
	}

	@Override
	public Completable initialize() {
		return loadConfig(KeycloakPluginConfig.class).map(config -> {
			this.publicKeys = loadPublicKeys(config);
			this.mapperScript = loadMapperScript(config);
			return config;
		}).ignoreElement();
	}

	private String loadMapperScript(KeycloakPluginConfig config) throws IOException {
		return FileUtils.readFileToString(new File(getStorageDir(), MAPPER_SCRIPT_NAME));
	}

	public MappingResult evaluateMapper(HttpServerRequest req, String uuid, JsonObject token) {
		Binding binding = new Binding();
		binding.setVariable("request", req);
		binding.setVariable("uuid", uuid);
		binding.setVariable("token", token);

		GroovyShell shell = new GroovyShell(binding);

		Object value = shell.evaluate(mapperScript);
		if (value instanceof MappingResult) {
			return (MappingResult) value;
		} else {
			log.error("The mapper script did not return the expected {" + MappingResult.class.getName() + "}");
			throw new RuntimeException("Mapper script failed for {" + uuid + "}");
		}
	}

	@Override
	public MappingResult mapToken(HttpServerRequest req, String uuid, JsonObject token) {
		return evaluateMapper(req, uuid, token);
		// MappingResult result = new MappingResult();
		//
		// if (uuid == null) {
		// log.info("First time login of the user");
		// } else {
		// log.info("Already synced user is logging in.");
		// }
		//
		// log.info("Mapping user in plugin");
		// printToken(token);
		// String username = token.getString("preferred_username");
		// UserUpdateRequest user = new UserUpdateRequest();
		// user.setUsername(username);
		// user.setEmailAddress("mapped@email.tld");
		// user.setFirstname("mapepdFirstname");
		// user.setLastname("mapepdLastname");
		// result.setUser(user);
		//
		// log.info("Mapping groups in plugin");
		// List<GroupResponse> groupList = new ArrayList<>();
		// groupList.add(new GroupResponse().setName("group1"));
		// groupList.add(new GroupResponse()
		// .setName("group2")
		// .setRoles(Arrays.asList(new RoleReference().setName("role1"))));
		// groupList.add(new GroupResponse()
		// .setName("group3")
		// .setRoles(Arrays.asList(new RoleReference().setName("role1"), new RoleReference().setName("role2"))));
		// result.setGroups(groupList);
		//
		// log.info("Mapping role in plugin");
		// List<RoleResponse> roleList = new ArrayList<>();
		// roleList.add(new RoleResponse().setName("role1"));
		// roleList.add(new RoleResponse().setName("role2"));
		// result.setRoles(roleList);
		//
		// result.setGroupFilter(groupName -> {
		// log.info("Handling removal of user from group {" + groupName + "}");
		// // Return true here if you want to remove the group with the given name
		// return false;
		// });
		//
		// result.setRoleFilter((groupName, roleName) -> {
		// log.info("Handling removal of role {" + roleName + "} from {" + groupName + "}");
		// // Return true here if you want to remove the role with the given name
		// return false;
		// });
		//
		// return result;
	}

	@Override
	public boolean acceptToken(HttpServerRequest httpServerRequest, JsonObject token) {
		log.info("Checking token. Accepting..");
		printToken(token);
		return true;
	}

	private void printToken(JsonObject token) {
		String username = token.getString("preferred_username");
		System.out.println("Token for {" + username + "}");
		System.out.println(token.encodePrettily());
	}

	/**
	 * Load the public keys for the configured realm from keycloak.
	 * 
	 * @param config
	 * @return
	 */
	private Set<JsonObject> loadPublicKeys(KeycloakPluginConfig config) {
		try {
			URL url = new URL(config.getUrl());
			String proto = url.getProtocol();
			String host = url.getHost();
			int port = url.getPort();
			if (port == -1) {
				port = proto.equalsIgnoreCase("https") ? 443 : 80;
			}
			String realmName = config.getRealmName();
			Set<JsonObject> keys = KeycloakUtils.loadJWKs(proto, host, port, realmName);
			if (log.isDebugEnabled()) {
				for (JsonObject key : keys) {
					log.debug("Loaded public key {\n" + key.encodePrettily() + "\n}");
				}
			}
			return keys;
		} catch (Exception e) {
			log.error("Error while loading public keys", e);
		}
		return Collections.emptySet();
	}

}
