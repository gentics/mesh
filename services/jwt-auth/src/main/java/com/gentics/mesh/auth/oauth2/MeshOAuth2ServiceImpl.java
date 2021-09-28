package com.gentics.mesh.auth.oauth2;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static com.google.common.base.Throwables.getRootCause;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.auth.AuthHandlerContainer;
import com.gentics.mesh.auth.AuthServicePluginRegistry;
import com.gentics.mesh.auth.MeshOAuthService;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.auth.GroupFilter;
import com.gentics.mesh.plugin.auth.MappingResult;
import com.gentics.mesh.plugin.auth.RoleFilter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.impl.JWTUser;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;

/**
 * @see MeshOAuthService
 */
@Singleton
public class MeshOAuth2ServiceImpl implements MeshOAuthService {

	private static final Logger log = LoggerFactory.getLogger(MeshOAuth2ServiceImpl.class);

	private static final String DEFAULT_JWT_USERNAME_PROP = "preferred_username";

	/**
	 * Cache the token id which was last used by an user.
	 */
	public final Cache<String, String> TOKEN_ID_LOG = Caffeine.newBuilder().maximumSize(20_000).expireAfterWrite(24, TimeUnit.HOURS).build();

	protected AuthServicePluginRegistry authPluginRegistry;
	protected Database db;
	protected BootstrapInitializer boot;
	protected PermissionRoots permissionRoots;
	private final AuthenticationOptions authOptions;
	private final Provider<EventQueueBatch> batchProvider;

	private final AuthHandlerContainer authHandlerContainer;
	private final LocalConfigApi localConfigApi;

	private final RequestDelegator delegator;

	@Inject
	public MeshOAuth2ServiceImpl(Database db, BootstrapInitializer boot, MeshOptions meshOptions,
		Provider<EventQueueBatch> batchProvider, AuthServicePluginRegistry authPluginRegistry,
		AuthHandlerContainer authHandlerContainer, LocalConfigApi localConfigApi, RequestDelegator delegator, PermissionRoots permissionRoots) {
		this.db = db;
		this.boot = boot;
		this.batchProvider = batchProvider;
		this.authPluginRegistry = authPluginRegistry;
		this.authOptions = meshOptions.getAuthenticationOptions();
		this.authHandlerContainer = authHandlerContainer;
		this.localConfigApi = localConfigApi;
		this.delegator = delegator;
		this.permissionRoots = permissionRoots;
	}

	private JWTAuthHandler createJWTHandler() {
		// Add the already configured public keys to the config.
		Set<JsonObject> keys = new HashSet<>();

		// 1. Add keys from config
		keys.addAll(authOptions.getPublicKeys().stream()
			.collect(Collectors.toSet()));

		// 2. Add keys from plugins
		keys.addAll(authPluginRegistry.getActivePublicKeys());

		// No need to handle the token since we have no keys setup.
		if (keys.isEmpty()) {
			return null;
		}

		if (log.isDebugEnabled()) {
			for (JsonObject key : keys) {
				if (key != null) {
					log.debug(key.encodePrettily());
				} else {
					log.debug("Key is null");
				}
			}
		}

		return authHandlerContainer.create(keys);
	}

	@Override
	public void secure(Route route) {
		// Add handler to decode and authenticate external JWT's
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
				log.warn("No key handler was created. This may happen when neither the plugin or the mesh configuration provides custom public keys.");
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
				JsonObject decodedToken = token.principal();
				for (AuthServicePlugin plugin : plugins) {
					if (!plugin.acceptToken(rc.request(), decodedToken)) {
						if (log.isDebugEnabled()) {
							log.debug("The plugin {} rejected the token.", plugin.getManifest().getName());
						}
						rc.fail(401);
						return;
					}
				}
				syncUser(rc, token.principal()).subscribe(syncedUser -> {
					rc.setUser(syncedUser);
					rc.next();
				}, error -> {
					if (getRootCause(error) instanceof CannotWriteException) {
						delegator.redirectToMaster(rc);
					} else {
						rc.fail(error);
					}
				});
			} else {
				rc.fail(401);
				return;
			}
		});
	}

	/**
	 * Utilize the user information to return the matching mesh user.
	 * 
	 * @param rc
	 * @param token
	 * @return
	 */
	protected Single<MeshAuthUser> syncUser(RoutingContext rc, JsonObject token) {
		Optional<String> usernameOpt = extractUsername(token);
		if (!usernameOpt.isPresent()) {
			throw new RuntimeException(
				"The username could not be determined. Maybe no plugin was able to return a match or the token did not contain the "
					+ DEFAULT_JWT_USERNAME_PROP + " property.");
		}
		String username = usernameOpt.get();
		String currentTokenId = token.getString("jti");
		// The JTI is optional - We use the username + iat if it is missing
		if (currentTokenId == null) {
			Integer iat = token.getInteger("iat");
			if (iat == null) {
				throw new RuntimeException("The token does not contain an iat or jti property. One of those values is required.");
			}
			currentTokenId = username + "-" + String.valueOf(iat);
		}
		String cachingId = currentTokenId;

		EventQueueBatch batch = batchProvider.get();
		return db.maybeTx(tx -> boot.userDao().findMeshAuthUserByUsername(username))
			.flatMapSingleElement(user -> db.singleTx(user.getDelegate()::getUuid).flatMap(uuid -> {
				// Compare the stored and current token id to see whether the current token is different.
				// In that case a sync must be invoked.
				String lastSeenTokenId = TOKEN_ID_LOG.getIfPresent(user.getDelegate().getUuid());
				if (lastSeenTokenId == null || !lastSeenTokenId.equals(cachingId)) {
					return assertReadOnlyDeactivated().andThen(db.singleTx(() -> {
						HibUser admin = boot.userDao().findByUsername("admin");
						runPlugins(rc, batch, admin, user, uuid, token);
						TOKEN_ID_LOG.put(uuid, cachingId);
						return user;
					}));
				} else {
					log.debug("The request does not need mapping since we have already processed the token before.");
				}
				return Single.just(user);
			}))
			// Create the user if it can't be found.
			.switchIfEmpty(
				assertReadOnlyDeactivated()
					.andThen(requiresWriteCompletable())
					.andThen(db.singleTxWriteLock(tx -> {
						UserDao userDao = tx.userDao();
						HibBaseElement userRoot = permissionRoots.user();
						HibUser admin = userDao.findByUsername("admin");
						HibUser createdUser = userDao.create(username, admin);
						userDao.inheritRolePermissions(admin, userRoot, createdUser);

						MeshAuthUser user = userDao.findMeshAuthUserByUsername(username);
						String uuid = user.getDelegate().getUuid();
						batch.add(user.getDelegate().onCreated());
						// Not setting uuid since the user has not yet been committed.
						runPlugins(rc, batch, admin, user, null, token);
						TOKEN_ID_LOG.put(uuid, cachingId);
						return user;
					})))
			.doOnSuccess(ignore -> batch.dispatch());
	}

	private Completable assertReadOnlyDeactivated() {
		return localConfigApi.getActiveConfig()
			.flatMapCompletable(config -> {
				if (config.isReadOnly()) {
					return Completable.error(error(METHOD_NOT_ALLOWED, "error_readonly_mode_oauth"));
				} else {
					return Completable.complete();
				}
			});
	}

	private Optional<String> extractUsername(JsonObject token) {
		List<AuthServicePlugin> plugins = authPluginRegistry.getPlugins();
		for (AuthServicePlugin plugin : plugins) {
			Optional<String> optionalUsername = plugin.extractUsername(token);
			if (optionalUsername.isPresent()) {
				return optionalUsername;
			}
		}
		// Finally lets try the default
		return Optional.ofNullable(token.getString(DEFAULT_JWT_USERNAME_PROP));
	}

	private void defaultUserMapper(EventQueueBatch batch, MeshAuthUser user, JsonObject token) throws CannotWriteException {
		boolean modified = false;
		String givenName = token.getString("given_name");
		if (givenName == null) {
			log.warn("Did not find given_name property in OAuth2 principle.");
		} else {
			String currentFirstName = user.getDelegate().getFirstname();
			if (!Objects.equals(currentFirstName, givenName)) {
				requiresWrite();
				user.getDelegate().setFirstname(givenName);
				modified = true;
			}
		}

		String familyName = token.getString("family_name");
		if (familyName == null) {
			log.warn("Did not find family_name property in OAuth2 principle.");
		} else {
			String currentLastName = user.getDelegate().getLastname();
			if (!Objects.equals(currentLastName, familyName)) {
				requiresWrite();
				user.getDelegate().setLastname(familyName);
				modified = true;
			}
		}

		String email = token.getString("email");
		if (email == null) {
			log.warn("Did not find email property in OAuth2 principle");
		} else {
			String currentEmail = user.getDelegate().getEmailAddress();
			if (!Objects.equals(currentEmail, email)) {
				requiresWrite();
				user.getDelegate().setEmailAddress(email);
				modified = true;
			}
		}
		if (modified) {
			batch.add(user.getDelegate().onUpdated());
		}
	}

	/**
	 * Runs all {@link AuthServicePlugin} to detect and apply any changes that are returned from
	 * {@link AuthServicePlugin#mapToken(HttpServerRequest, String, JsonObject)}.
	 *
	 * @param rc
	 * @param batch
	 * @param admin
	 * @param user
	 * @param userUuid
	 * @param token
	 * @throws CannotWriteException
	 *             If a change is required but this instance cannot be written to because of cluster coordination.
	 * @return
	 */
	private void runPlugins(RoutingContext rc, EventQueueBatch batch, HibUser admin, MeshAuthUser user, String userUuid,
		JsonObject token) throws CannotWriteException {
		List<AuthServicePlugin> plugins = authPluginRegistry.getPlugins();
		// Only load the needed data for plugins if there are any plugins
		if (!plugins.isEmpty()) {
			RoleDao roleDao = boot.roleDao();
			GroupDao groupDao = boot.groupDao();
			UserDao userDao = boot.userDao();

			HibBaseElement groupRoot = permissionRoots.group();
			HibBaseElement roleRoot = permissionRoots.role();

			for (AuthServicePlugin plugin : plugins) {
				if (log.isDebugEnabled()) {
					log.debug("Handling mapping via auth plugin {}", plugin.getManifest().getName());
				}
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
						if (!delegator.canWrite() && userDao.updateDry(user.getDelegate(), ac)) {
							throw new CannotWriteException();
						}
						userDao.update(user.getDelegate(), ac, batch);
					} else {
						defaultUserMapper(batch, user, token);
						continue;
					}

					// 2. Map the roles
					List<RoleResponse> mappedRoles = result.getRoles();
					if (mappedRoles != null) {
						for (RoleResponse mappedRole : mappedRoles) {
							String roleUuid = mappedRole.getUuid();
							String roleName = mappedRole.getName();
							HibRole role = null;
							if (roleUuid != null) {
								role = roleDao.findByUuid(roleUuid);
							} else if (roleName != null) {
								role = roleDao.findByName(roleName);
							}

							// Role not found - Lets create it
							if (role == null) {
								if (roleName != null) {
									requiresWrite();
									log.debug("Creating new role {} via mapping request.", roleName);
									role = roleDao.create(roleName, admin);
									userDao.inheritRolePermissions(admin, roleRoot, role);
									batch.add(role.onCreated());
								} else {
									log.error("Unable to create role. No role name was specified.");
								}
							}
						}
					}

					// 3. Map the groups
					List<GroupResponse> mappedGroups = result.getGroups();
					if (mappedGroups != null) {

						// Now create the groups and assign the user to the listed groups
						for (GroupResponse mappedGroup : mappedGroups) {
							String groupName = mappedGroup.getName();
							String groupUuid = mappedGroup.getUuid();
							HibGroup group = null;

							if (groupUuid != null) {
								group = groupDao.findByUuid(groupUuid);
							} else if (groupName != null) {
								group = groupDao.findByName(groupName);
							}

							// Group not found - Lets create it
							boolean created = false;
							if (group == null) {
								if (groupName != null) {
									requiresWrite();
									log.debug("Creating group {} via mapping request.", groupName);
									group = groupDao.create(groupName, admin);
									userDao.inheritRolePermissions(admin, groupRoot, group);
									batch.add(group.onCreated());
									created = true;
								} else {
									log.error("Unable to create group. No group name was specified.");
									// We can't continue with this mapped group.
									break;
								}
							}
							if (!groupDao.hasUser(group, user.getDelegate())) {
								requiresWrite();
								log.debug("Adding user {} to group {} via mapping request.", user.getDelegate().getUsername(), group.getName());
								// Ensure that the user is part of the group
								groupDao.addUser(group, user.getDelegate());
								batch.add(groupDao.createUserAssignmentEvent(group, user.getDelegate(), ASSIGNED));
								// We only need one event
								if (!created) {
									batch.add(group.onUpdated());
								}
							}
							// 4. Assign roles to groups
							for (RoleReference assignedRole : mappedGroup.getRoles()) {
								String roleName = assignedRole.getName();
								String roleUuid = assignedRole.getUuid();
								HibRole role = null;

								if (roleName != null) {
									role = roleDao.findByName(roleName);
								} else if (roleUuid != null) {
									role = roleDao.findByUuid(roleUuid);
								}

								// Add the role if it is missing
								if (role != null && !groupDao.hasRole(group, role)) {
									requiresWrite();
									log.debug("Adding role {} to group {} via mapping request.", role.getName(), group.getName());
									groupDao.addRole(group, role);
									group.setLastEditedTimestamp();
									group.setEditor(admin);
									batch.add(groupDao.createRoleAssignmentEvent(group, role, ASSIGNED));
								} else {
									log.warn("Unable to map role to group. The role with name {} / uuid {} could not be found", roleName, roleUuid);
								}
							}

							// 5. Check if the plugin wants to remove any of the roles from the mapped group.
							RoleFilter roleFilter = result.getRoleFilter();
							if (roleFilter != null) {
								for (HibRole role : groupDao.getRoles(group)) {
									if (roleFilter.filter(group.getName(), role.getName())) {
										requiresWrite();
										log.info("Unassigning role {" + role.getName() + "} from group {" + group.getName() + "}");
										groupDao.removeRole(group, role);
										batch.add(groupDao.createRoleAssignmentEvent(group, role, UNASSIGNED));
									}
								}
							}
						}
					}

					// 6. Now check the roles again and handle their group assignments
					if (mappedRoles != null) {
						for (RoleResponse mappedRole : mappedRoles) {
							String roleName = mappedRole.getName();
							HibRole role = roleDao.findByName(roleName);

							if (role == null) {
								log.warn("Could not find referenced role {" + role + "}");
								continue;
							}
							// Assign groups to roles
							for (GroupReference assignedGroup : mappedRole.getGroups()) {
								String groupName = assignedGroup.getName();
								String groupUuid = assignedGroup.getUuid();
								HibGroup group = null;

								if (groupName != null) {
									group = groupDao.findByName(groupName);
								} else if (groupUuid != null) {
									group = groupDao.findByUuid(groupUuid);
								}

								// Add the role if it is missing
								if (group != null && !groupDao.hasRole(group, role)) {
									requiresWrite();
									groupDao.addRole(group, role);
									batch.add(groupDao.createRoleAssignmentEvent(group, role, ASSIGNED));
								} else {
									log.error("Could not find group in role->group mapping of role {}", role.getName());
								}
							}

						}
					}

					// 7. Check if the plugin wants to remove the user from any of its current groups.
					GroupFilter groupFilter = result.getGroupFilter();
					if (groupFilter != null) {
						for (HibGroup group : userDao.getGroups(user.getDelegate())) {
							if (groupFilter.filter(group.getName())) {
								requiresWrite();
								log.info("Unassigning group {" + group.getName() + "} from user {" + user.getDelegate().getUsername() + "}");
								groupDao.removeUser(group, user.getDelegate());
								batch.add(groupDao.createUserAssignmentEvent(group, user.getDelegate(), UNASSIGNED));
							}
						}
					}

				} catch (CannotWriteException e) {
					throw e;
				} catch (Exception e) {
					log.error("Error while executing mapping plugin {" + plugin.id() + "}. Ignoring result.", e);
					throw e;
				}
			}
		} else {
			log.debug("No auth plugins could be found. Falling back to default mapping");
			defaultUserMapper(batch, user, token);
		}

	}

	private void requiresWrite() throws CannotWriteException {
		if (!delegator.canWrite()) {
			throw new CannotWriteException();
		}
	}

	private Completable requiresWriteCompletable() {
		if (delegator.canWrite()) {
			return Completable.complete();
		} else {
			return Completable.error(new CannotWriteException());
		}
	}
}
