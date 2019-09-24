package com.gentics.mesh.auth;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
import com.gentics.mesh.etc.config.auth.JsonWebKey;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.auth.GroupFilter;
import com.gentics.mesh.plugin.auth.MappingResult;
import com.gentics.mesh.plugin.auth.RoleFilter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.impl.JWTUser;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

@Singleton
@SuppressWarnings("restriction")
public class MeshOAuth2ServiceImpl implements MeshOAuthService {

	private static final Logger log = LoggerFactory.getLogger(MeshOAuth2ServiceImpl.class);

	/**
	 * Cache the token id which was last used by an user.
	 */
	public static final Cache<String, String> TOKEN_ID_LOG = Caffeine.newBuilder().maximumSize(20_000).expireAfterWrite(24, TimeUnit.HOURS).build();

	protected AuthServicePluginRegistry authPluginRegistry;
	protected NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
	protected Database db;
	protected BootstrapInitializer boot;
	private final AuthenticationOptions authOptions;
	private final Provider<EventQueueBatch> batchProvider;

	private final AuthHandlerContainer authHandlerContainer;

	@Inject
	public MeshOAuth2ServiceImpl(Database db, BootstrapInitializer boot, MeshOptions meshOptions,
		Provider<EventQueueBatch> batchProvider, AuthServicePluginRegistry authPluginRegistry, AuthHandlerContainer authHandlerContainer) {
		this.db = db;
		this.boot = boot;
		this.batchProvider = batchProvider;
		this.authPluginRegistry = authPluginRegistry;
		this.authOptions = meshOptions.getAuthenticationOptions();
		this.authHandlerContainer = authHandlerContainer;
	}

	private JWTAuthHandler createJWTHandler() {
		// Add the already configured public keys to the config.
		Set<JsonWebKey> keys = new HashSet<>();

		// 1. Add keys from config
		keys.addAll(authOptions.getPublicKeys().stream()
			.map(JsonObject::new)
			.map(JsonWebKey::create)
			.collect(Collectors.toSet()));

		// 2. Add keys from plugins
		keys.addAll(authPluginRegistry.getActivePublicKeys());

		// No need to handle the token since we have no keys setup.
		if (keys.isEmpty()) {
			return null;
		}

		System.out.println("Keys: " + keys.size());
		if (log.isDebugEnabled()) {
			for (JsonWebKey key : keys) {
				if (key != null) {
					log.debug(key.toJson().encodePrettily());
				} else {
					log.debug("Key is null");
				}
			}
		}

		return authHandlerContainer.create(keys);
	}

	@Override
	public void secure(Route route) {
		route.handler(rc -> {
			if (rc.user() != null) {
				rc.next();
				return;
			}
			final String authorization = rc.request().headers().get(HttpHeaders.AUTHORIZATION);

			// No need to handle checks. The auth handler will fail
			if (authorization == null) {
				rc.next();
				return;
			}
			JWTAuthHandler keyHandler = createJWTHandler();
			if (keyHandler != null) {
				keyHandler.handle(rc);
			} else {
				rc.next();
			}
		});

		// Check whether the oauth handler was successful and convert the user to a mesh user.
		route.handler(rc -> {
			User user = rc.user();
			if (user == null) {
				rc.next();
				return;
			}
			if (user instanceof MeshAuthUser) {
				rc.next();
				return;
			}
			if (user instanceof JWTUser) {
				JWTUser token = (JWTUser) user;

				List<AuthServicePlugin> plugins = authPluginRegistry.getPlugins();
				for (AuthServicePlugin plugin : plugins) {
					if (!plugin.acceptToken(rc.request(), token.principal())) {
						rc.fail(401);
						return;
					}
				}
				rc.setUser(syncUser(rc, token.principal()));

			} else {
				rc.fail(401);
				return;
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
		System.out.println(token.encodePrettily());
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

}
