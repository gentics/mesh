package com.gentics.mesh.auth;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OAuth2Options;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.auth.GroupFilter;
import com.gentics.mesh.plugin.auth.MappingResult;
import com.gentics.mesh.plugin.auth.RoleFilter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
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

	protected AuthServicePluginRegistry authPluginRegistry;
	protected MeshOAuth2AuthHandlerImpl oauth2Handler;
	protected NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
	protected OAuth2Options options;
	protected OAuth2Auth oauth2Provider;
	protected Database db;
	protected BootstrapInitializer boot;

	private final Provider<EventQueueBatch> batchProvider;

	@Inject
	public MeshOAuth2ServiceImpl(Database db, BootstrapInitializer boot, MeshOptions meshOptions, Vertx vertx,
		Provider<EventQueueBatch> batchProvider, AuthServicePluginRegistry authPluginRegistry) {
		this.db = db;
		this.boot = boot;
		this.batchProvider = batchProvider;
		this.authPluginRegistry = authPluginRegistry;
		this.options = meshOptions.getAuthenticationOptions().getOauth2();
		if (options == null || !options.isEnabled()) {
			return;
		}

		JsonObject config = loadRealmInfo(vertx, meshOptions);

		this.oauth2Provider = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, config);
		this.oauth2Handler = new MeshOAuth2AuthHandlerImpl(oauth2Provider);

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
					List<AuthServicePlugin> plugins = authPluginRegistry.getPlugins();
					for (AuthServicePlugin plugin : plugins) {
						if (!plugin.acceptToken(rc.request(), token.accessToken())) {
							rc.fail(401);
							return;
						}
					}
					rc.setUser(syncUser(rc, token.accessToken()));
				}
			}
			rc.next();
		});
	}

	/**
	 * Utilize the user information to return the matching mesh user.
	 * 
	 * @param rc
	 * @param token
	 * @return
	 */
	protected MeshAuthUser syncUser(RoutingContext rc, JsonObject token) {
		String username = token.getString("preferred_username");
		Objects.requireNonNull(username, "The preferred_username property could not be found in the principle user info.");
		String currentTokenId = token.getString("jti");

		EventQueueBatch batch = batchProvider.get();
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
				// Not setting uuid since the user has not yet been comitted.
				runPlugins(rc, batch, admin, user, null, token);
				TOKEN_ID_LOG.put(uuid, currentTokenId);
			} else {
				// Compare the stored and current token id to see whether the current token is different.
				// In that case a sync must be invoked.
				String uuid = user.getUuid();
				String lastSeenTokenId = TOKEN_ID_LOG.getIfPresent(user.getUuid());
				if (lastSeenTokenId == null || !lastSeenTokenId.equals(currentTokenId)) {
					com.gentics.mesh.core.data.User admin = root.findByUsername("admin");
					runPlugins(rc, batch, admin, user, uuid, token);
					TOKEN_ID_LOG.put(uuid, currentTokenId);
				}
			}
			return user;
		});
		batch.dispatch();
		return authUser;

	}

	private void defaultUserMapper(EventQueueBatch batch, MeshAuthUser user, JsonObject token) {
		boolean modified = false;
		String givenName = token.getString("given_name");
		if (givenName == null) {
			log.warn("Did not find given_name property in OAuth2 principle.");
		} else {
			String currentFirstName = user.getFirstname();
			if (!Objects.equals(currentFirstName, givenName)) {
				user.setFirstname(givenName);
				modified = true;
			}
		}

		String familyName = token.getString("family_name");
		if (familyName == null) {
			log.warn("Did not find family_name property in OAuth2 principle.");
		} else {
			String currentLastName = user.getLastname();
			if (!Objects.equals(currentLastName, familyName)) {
				user.setLastname(familyName);
				modified = true;
			}
		}

		String email = token.getString("email");
		if (email == null) {
			log.warn("Did not find email property in OAuth2 principle");
		} else {
			String currentEmail = user.getEmailAddress();
			if (!Objects.equals(currentEmail, email)) {
				user.setEmailAddress(email);
				modified = true;
			}
		}
		if (modified) {
			batch.add(user.onUpdated());
		}

	}

	private void runPlugins(RoutingContext rc, EventQueueBatch batch, com.gentics.mesh.core.data.User admin, MeshAuthUser user, String userUuid,
		JsonObject token) {
		List<AuthServicePlugin> plugins = authPluginRegistry.getPlugins();
		// Only load the needed data for plugins if there are any plugins
		if (!plugins.isEmpty()) {
			RoleRoot roleRoot = boot.roleRoot();
			GroupRoot groupRoot = boot.groupRoot();

			for (AuthServicePlugin plugin : plugins) {
				try {
					MappingResult result = plugin.mapToken(rc.request(), userUuid, token);
					// Just invoke the default mapper if the plugin provides no mapping
					if (result == null) {
						log.debug("Plugin did not provide a mapping result. Using only default mapping for user");
						defaultUserMapper(batch, user, token);
						continue;
					}

					// 1. Map the user
					UserUpdateRequest mappedUser = result.getUser();
					if (mappedUser != null) {
						InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
						ac.setBody(mappedUser);
						ac.setUser(admin.toAuthUser());
						user.update(ac, batch);
					} else {
						defaultUserMapper(batch, user, token);
					}

					// 2. Map the roles
					List<RoleResponse> mappedRoles = result.getRoles();
					if (mappedRoles != null) {
						for (RoleResponse mappedRole : mappedRoles) {
							String roleName = mappedRole.getName();
							Role role = roleRoot.findByName(roleName);
							// Role not found - Lets create it
							if (role == null) {
								role = roleRoot.create(roleName, admin);
								admin.addCRUDPermissionOnRole(roleRoot, CREATE_PERM, role);
								batch.add(role.onCreated());
							}
						}
					}

					// 3. Map the groups
					List<GroupResponse> mappedGroups = result.getGroups();
					if (mappedGroups != null) {

						// Now create the groups and assign the user to the listed groups
						for (GroupResponse mappedGroup : mappedGroups) {
							String groupName = mappedGroup.getName();
							Group group = groupRoot.findByName(groupName);

							// Group not found - Lets create it
							boolean created = false;
							if (group == null) {
								group = groupRoot.create(groupName, admin);
								admin.addCRUDPermissionOnRole(groupRoot, CREATE_PERM, group);
								batch.add(group.onCreated());
								created = true;
							}
							if (!group.hasUser(user)) {
								// Ensure that the user is part of the group
								group.addUser(user);
								batch.add(group.createUserAssignmentEvent(user, ASSIGNED));
								// We only need one event
								if (!created) {
									batch.add(group.onUpdated());
								}
							}
							// 4. Assign roles to groups
							for (RoleReference assignedRole : mappedGroup.getRoles()) {
								String roleName = assignedRole.getName();
								String roleUuid = assignedRole.getUuid();
								Role role = null;
								// Try name
								if (roleName != null) {
									role = roleRoot.findByName(roleName);
								}
								// Try uuid
								if (role == null) {
									role = roleRoot.findByUuid(roleUuid);
								}
								// Add the role if it is missing
								if (role != null && !group.hasRole(role)) {
									group.addRole(role);
									group.setLastEditedTimestamp();
									group.setEditor(admin);
									batch.add(group.createRoleAssignmentEvent(role, ASSIGNED));
								}
							}

							// 5. Check if the plugin wants to remove any of the roles from the mapped group.
							RoleFilter roleFilter = result.getRoleFilter();
							if (roleFilter != null) {
								for (Role role : group.getRoles()) {
									if (roleFilter.filter(group.getName(), role.getName())) {
										log.info("Unassigning role {" + role.getName() + "} from group {" + group.getName() + "}");
										group.removeRole(role);
										batch.add(group.createRoleAssignmentEvent(role, UNASSIGNED));
									}
								}
							}
						}
					}

					// 6. Now check the roles again and handle their group assignments
					if (mappedRoles != null) {
						for (RoleResponse mappedRole : mappedRoles) {
							String roleName = mappedRole.getName();
							Role role = roleRoot.findByName(roleName);

							if (role == null) {
								log.warn("Could not find referenced role {" + role + "}");
								continue;
							}
							// Assign groups to roles
							for (GroupReference assignedGroup : mappedRole.getGroups()) {
								String groupName = assignedGroup.getName();
								String groupUuid = assignedGroup.getUuid();
								Group group = null;
								// Try name
								if (groupName != null) {
									group = groupRoot.findByName(groupName);
								}
								// Try uuid
								if (group == null) {
									group = groupRoot.findByUuid(groupUuid);
								}
								// Add the role if it is missing
								if (group != null && !group.hasRole(role)) {
									group.addRole(role);
									batch.add(group.createRoleAssignmentEvent(role, ASSIGNED));
								}
							}

						}
					}

					// 7. Check if the plugin wants to remove the user user from any of its current groups.
					GroupFilter groupFilter = result.getGroupFilter();
					if (groupFilter != null) {
						for (Group group : user.getGroups()) {
							if (groupFilter.filter(group.getName())) {
								log.info("Unassigning group {" + group.getName() + "} from user {" + user.getUsername() + "}");
								group.removeUser(user);
								batch.add(group.createUserAssignmentEvent(user, UNASSIGNED));
							}
						}
					}

				} catch (Exception e) {
					log.error("Error while executing mapping plugin {" + plugin.id() + "}. Ignoring result.", e);
					throw e;
				}
			}

		} else {
			defaultUserMapper(batch, user, token);
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
			log.debug("OAuth config not specified. Can't setup OAuth2.");
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

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				log.error(response.body().toString());

				throw new RuntimeException("Error while loading realm info. Got code {" + response.code() + "}");
			}
			return new JsonObject(response.body().string());
		}
	}
}
