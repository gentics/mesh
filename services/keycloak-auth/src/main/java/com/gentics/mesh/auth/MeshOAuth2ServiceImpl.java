package com.gentics.mesh.auth;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OAuth2Options;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Route;
import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;

@Singleton
@SuppressWarnings("restriction")
public class MeshOAuth2ServiceImpl implements MeshOAuthService {

	private static final Logger log = LoggerFactory.getLogger(MeshOAuth2ServiceImpl.class);

	/**
	 * Cache the token id which was last used by an user.
	 */
	public static final Cache<String, String> TOKEN_ID_LOG = Caffeine.newBuilder().maximumSize(20_000).expireAfterWrite(24, TimeUnit.HOURS).build();

	protected MeshOAuth2AuthHandlerImpl oauth2Handler;
	protected NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
	protected OAuth2Options options;
	protected String mapperScript = null;
	protected OAuth2Auth oauth2Provider;
	protected Database db;
	protected BootstrapInitializer boot;

	@Inject
	public MeshOAuth2ServiceImpl(Database db, BootstrapInitializer boot, MeshOptions meshOptions, Vertx vertx) {
		this.db = db;
		this.boot = boot;
		this.options = meshOptions.getAuthenticationOptions().getOauth2();
		if (options == null || !options.isEnabled()) {
			return;
		}

		JsonObject config = loadRealmInfo(vertx, meshOptions);
		this.mapperScript = loadScript();

		this.oauth2Provider = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, config);
		this.oauth2Handler = new MeshOAuth2AuthHandlerImpl(oauth2Provider);

	}

	/**
	 * Read the mapper script from the configured path.
	 * 
	 * @return
	 */
	protected String loadScript() {
		String path = options.getMapperScriptPath();
		if (path == null) {
			return null;
		}
		File scriptFile = new File(path);
		if (scriptFile.exists()) {
			try {
				return FileUtils.readFileToString(scriptFile, Charset.defaultCharset());
			} catch (IOException e) {
				throw error(INTERNAL_SERVER_ERROR, "oauth_mapper_file_not_readable", e);
			}
		} else {
			log.warn("The OAuth2 mapper script {" + path + "} could not be found.");
			return null;
		}
	}

	protected JsonObject executeMapperScript(JsonObject principle) throws ScriptException {
		JsonObject info = new JsonObject();
		info.put("roles", new JsonArray());
		info.put("groups", new JsonArray());

		boolean devMode = options.isMapperScriptDevMode();
		if (devMode) {
			log.info("Reloading mapper script due to enabled development mode.");
			mapperScript = loadScript();
		}

		if (StringUtils.isEmpty(mapperScript)) {
			return info;
		}

		ScriptEngine engine = factory.getScriptEngine(new Sandbox());
		engine.put("principle", principle);
		StringBuilder script = new StringBuilder();
		script.append(mapperScript);
		script.append("\ngroups = JSON.stringify(extractGroups(JSON.parse(principle)));");
		script.append("\nroles = JSON.stringify(extractRoles(JSON.parse(principle)));");

		if (devMode || log.isDebugEnabled()) {
			log.info("Executing mapper script:\n" + script.toString());
			log.info("Using principle:\n" + principle.encodePrettily());
		}

		engine.eval(script.toString());
		Object rolesResult = engine.get("roles");
		if (rolesResult != null) {
			if (rolesResult instanceof String) {
				info.put("roles", new JsonArray((String) rolesResult));
			} else {
				throw new RuntimeException("The mapper script must return roles as a string. Got {" + rolesResult.getClass().getName() + "}");
			}
		}

		Object groupResult = engine.get("groups");
		if (groupResult != null) {
			if (groupResult instanceof String) {
				info.put("groups", new JsonArray((String) groupResult));
			} else {
				throw new RuntimeException("The mapper script must return groups as a string. Got {" + groupResult.getClass().getName() + "}");
			}
		}
		if (devMode || log.isDebugEnabled()) {
			log.info("Mapping script output:\n" + info.encodePrettily());
		}
		return info;
	}

	@Override
	public void secure(Route route) {
		route.handler(oauth2Handler);

		// Check whether the oauth handler was successful and convert the user to a mesh user.
		route.handler(rc -> {
			User user = rc.user();
			if (user instanceof AccessToken) {
				// FIXME - Workaround for Vert.x bug - https://github.com/vert-x3/vertx-auth/issues/216
				AccessToken token = (AccessToken) user;
				if (token.accessToken() == null) {
					rc.fail(401);
					return;
				} else {
					rc.setUser(syncUser(token.accessToken()));
				}
			}
			rc.next();
		});
	}

	/**
	 * Utilize the user information to return the matching mesh user.
	 * 
	 * @param userInfo
	 * @return
	 */
	protected MeshAuthUser syncUser(JsonObject userInfo) {
		String username = userInfo.getString("preferred_username");
		Objects.requireNonNull(username, "The preferred_username property could not be found in the principle user info.");
		String currentTokenId = userInfo.getString("jti");

		EventQueueBatch batch = EventQueueBatch.create();
		MeshAuthUser authUser = db.tx(() -> {
			UserRoot root = boot.userRoot();
			MeshAuthUser user = root.findMeshAuthUserByUsername(username);
			// Create the user if it can't be found.
			if (user == null) {
				com.gentics.mesh.core.data.User admin = root.findByUsername("admin");
				com.gentics.mesh.core.data.User createdUser = root.create(username, admin);
				admin.addCRUDPermissionOnRole(root, CREATE_PERM, createdUser);

				user = root.findMeshAuthUserByUsername(username);
				String uuid = user.getUuid();
				syncUser(batch, user, admin, userInfo);
				TOKEN_ID_LOG.put(uuid, currentTokenId);
			} else {
				// Compare the stored and current token id to see whether the current token is different.
				// In that case a sync must be invoked.
				String uuid = user.getUuid();
				String lastSeenTokenId = TOKEN_ID_LOG.getIfPresent(user.getUuid());
				if (lastSeenTokenId == null || !lastSeenTokenId.equals(currentTokenId)) {
					com.gentics.mesh.core.data.User admin = root.findByUsername("admin");
					syncUser(batch, user, admin, userInfo);
					TOKEN_ID_LOG.put(uuid, currentTokenId);
				}
			}
			return user;
		});
		batch.dispatch();
		return authUser;

	}

	/**
	 * Synchronize the other components of the user (e.g.: roles, groups).
	 * 
	 * @param batch
	 * @param user
	 * @param admin
	 * @param userInfo
	 */
	protected void syncUser(EventQueueBatch batch, MeshAuthUser user, com.gentics.mesh.core.data.User admin, JsonObject userInfo) {
		String givenName = userInfo.getString("given_name");
		if (givenName == null) {
			log.warn("Did not find given_name property in OAuth2 principle.");
		} else {
			user.setFirstname(givenName);
		}

		String familyName = userInfo.getString("family_name");
		if (familyName == null) {
			log.warn("Did not find family_name property in OAuth2 principle.");
		} else {
			user.setLastname(familyName);
		}

		String email = userInfo.getString("email");
		if (email == null) {
			log.warn("Did not find email property in OAuth2 principle");
		} else {
			user.setEmailAddress(email);
		}
		batch.add(user.onUpdated());

		try {
			JsonObject mappingInfo = executeMapperScript(userInfo);
			JsonArray roles = mappingInfo.getJsonArray("roles");
			RoleRoot roleRoot = boot.roleRoot();
			for (int i = 0; i < roles.size(); i++) {
				String roleName = roles.getString(i);
				Role role = roleRoot.findByName(roleName);
				if (role == null) {
					role = roleRoot.create(roleName, admin);
					admin.addCRUDPermissionOnRole(roleRoot, CREATE_PERM, role);
					batch.add(role.onUpdated());
				}
				// The group<->role assignment must be done manually. We don't want to enforce this here.
			}
			JsonArray groups = mappingInfo.getJsonArray("groups");
			GroupRoot groupRoot = boot.groupRoot();

			// First remove the user from all groups
			for (Group group : groupRoot.findAll()) {
				// TODO only remove the user if needed.
				// We should not touch other groups if not needed.
				// Otherwise the permission store and index sync gets a lot of work.
				group.removeUser(user);
				batch.add(group.onUpdated());
			}

			// Now create the groups and assign the user to the listed groups
			for (int i = 0; i < groups.size(); i++) {
				String groupName = groups.getString(i);
				Group group = groupRoot.findByName(groupName);
				if (group == null) {
					group = groupRoot.create(groupName, admin);
					admin.addCRUDPermissionOnRole(groupRoot, CREATE_PERM, group);
				}
				// Ensure that the user is part of the group
				group.addUser(user);
				batch.add(group.onUpdated());
			}
		} catch (Exception e) {
			log.error("Error while executing mapping script. Ignoring mapping script.", e);
		}
	}

	public OAuth2Auth getOauth2Provider() {
		return oauth2Provider;
	}

	/**
	 * Sandbox classfilter that filters all classes
	 */
	protected static class Sandbox implements ClassFilter {
		@Override
		public boolean exposeToScripts(String className) {
			return false;
		}
	}

	/**
	 * Load the settings and enhance them with the public realm information from the auth server.
	 * 
	 * @return
	 */
	public JsonObject loadRealmInfo(Vertx vertx, MeshOptions options) {
		if (options == null) {
			log.debug("Mesh options not specified. Can't setup OAuth2.");
			return null;
		}
		AuthenticationOptions authOptions = options.getAuthenticationOptions();
		if (authOptions == null) {
			log.debug("Mesh auth options not specified. Can't setup OAuth2.");
			return null;
		}
		JsonObject config = options.getAuthenticationOptions().getOauth2().getConfig().toJson();
		if (config == null) {
			log.debug("OAuth config  not specified. Can't setup OAuth2.");
			return null;
		}

		String realmName = config.getString("realm");
		Objects.requireNonNull(realmName, "The realm property was not found in the oauth2 config");
		String url = config.getString("auth-server-url");
		Objects.requireNonNull(realmName, "The auth-server-url property was not found in the oauth2 config");

		try {
			URL authServerUrl = new URL(url);
			String authServerHost = authServerUrl.getHost();
			config.put("auth-server-host", authServerHost);
			int authServerPort = authServerUrl.getPort();
			config.put("auth-server-port", authServerPort);
			String authServerProtocol = authServerUrl.getProtocol();
			config.put("auth-server-protocol", authServerProtocol);

			JsonObject json = fetchPublicRealmInfo(authServerProtocol, authServerHost, authServerPort, realmName);
			config.put("auth-server-url", authServerProtocol + "://" + authServerHost + ":" + authServerPort + "/auth");
			config.put("realm-public-key", json.getString("public_key"));
			return config;
		} catch (Exception e) {
			throw error(HttpResponseStatus.INTERNAL_SERVER_ERROR, "oauth_config_error", e);
		}

	}

	protected JsonObject fetchPublicRealmInfo(String protocol, String host, int port, String realmName) throws IOException {
		Builder builder = new OkHttpClient.Builder();
		OkHttpClient client = builder.build();

		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.url(protocol + "://" + host + ":" + port + "" + "/auth/realms/" + realmName)
			.build();

		Response response = client.newCall(request).execute();
		if (!response.isSuccessful()) {
			log.error(response.body().toString());
			throw new RuntimeException("Error while loading realm info. Got code {" + response.code() + "}");
		}
		return new JsonObject(response.body().string());

	}
}
