package com.gentics.mesh.auth.oauth2;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static com.google.common.base.Throwables.getRootCause;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.gentics.mesh.auth.util.MappingHelper;
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
import com.gentics.mesh.core.db.Tx;
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
	protected PermissionRoots permissionRoots;
	private final AuthenticationOptions authOptions;
	private final Provider<EventQueueBatch> batchProvider;

	private final AuthHandlerContainer authHandlerContainer;
	private final LocalConfigApi localConfigApi;

	private final RequestDelegator delegator;

	@Inject
	public MeshOAuth2ServiceImpl(Database db, MeshOptions meshOptions,
		Provider<EventQueueBatch> batchProvider, AuthServicePluginRegistry authPluginRegistry,
		AuthHandlerContainer authHandlerContainer, LocalConfigApi localConfigApi, RequestDelegator delegator, PermissionRoots permissionRoots) {
		this.db = db;
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
			List<AuthServicePlugin> plugins = authPluginRegistry.getPlugins();
			// The Vert.x JWT implementation stores the decoded token in attributes.accessToken. If that JsonObject is
			// missing or empty, something went wrong during decoding of the token. User.principal() only contains
			// some of the claims from the token, this is why it is not used here.
			JsonObject decodedToken = user.attributes().getJsonObject("accessToken");

			if (decodedToken == null || decodedToken.isEmpty()) {
				rc.fail(401);
				return;
			}

			for (AuthServicePlugin plugin : plugins) {
				if (!plugin.acceptToken(rc.request(), decodedToken)) {
					if (log.isDebugEnabled()) {
						log.debug("The plugin {} rejected the token.", plugin.getManifest().getName());
					}
					rc.fail(401);
					return;
				}
			}
			syncUser(rc, decodedToken).subscribe(syncedUser -> {
				rc.setUser(syncedUser);
				rc.next();
			}, error -> {
				// if the current instance is not writable, we redirect the request to the master instance
				Throwable rootCause = getRootCause(error);
				if (rootCause instanceof CannotWriteException) {
					delegator.redirectToMaster(rc);
				} else {
					// all other errors while sync'ing will cause the sync to be repeated once.
					// the reason is that the error might be caused by a race condition (when parallel requests try to sync the same objects)
					// trying again (once) increases the chance of the request to actually succeed.
					syncUser(rc, decodedToken).subscribe(syncedUser -> {
						rc.setUser(syncedUser);
						rc.next();
					}, rc::fail);
				}
			});
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
		return db.maybeTx(tx -> tx.userDao().findMeshAuthUserByUsername(username))
			.flatMapSingleElement(user -> db.singleTx(user.getDelegate()::getUuid).flatMap(uuid -> {
				// Compare the stored and current token id to see whether the current token is different.
				// In that case a sync must be invoked.
				String lastSeenTokenId = TOKEN_ID_LOG.getIfPresent(uuid);
				if (lastSeenTokenId == null || !lastSeenTokenId.equals(cachingId)) {
					return assertReadOnlyDeactivated().andThen(db.singleTx(tx -> {
						HibUser admin = tx.userDao().findByUsername("admin");
						runPlugins(tx, rc, batch, admin, user, uuid, token);
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
						// Not setting uuid since the user has not yet been committed.
						runPlugins(tx, rc, batch, admin, user, null, token);
						TOKEN_ID_LOG.put(uuid, cachingId);
						return user;
					})))
			.doOnSuccess(ignore -> batch.dispatch());
	}

	private Completable assertReadOnlyDeactivated() {
		if (db.isReadOnly(true)) {
			return Completable.error(error(METHOD_NOT_ALLOWED, "error_readonly_mode_oauth"));
		}
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
	 * @param tx
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
	private void runPlugins(Tx tx, RoutingContext rc, EventQueueBatch batch, HibUser admin, MeshAuthUser user, String userUuid,
		JsonObject token) throws CannotWriteException {
		List<AuthServicePlugin> plugins = authPluginRegistry.getPlugins();
		// Only load the needed data for plugins if there are any plugins
		if (!plugins.isEmpty()) {
			RoleDao roleDao = tx.roleDao();
			GroupDao groupDao = tx.groupDao();
			UserDao userDao = tx.userDao();

			HibBaseElement groupRoot = permissionRoots.group();
			HibBaseElement roleRoot = permissionRoots.role();
			HibUser authUser = userDao.findByUuid(user.getDelegate().getUuid());

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
						if (!delegator.canWrite() && userDao.updateDry(authUser, ac)) {
							throw new CannotWriteException();
						}
						userDao.update(authUser, ac, batch);
					} else {
						defaultUserMapper(batch, user, token);
						continue;
					}

					handleMappingResult(tx, batch, result, authUser, admin);
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

	/**
	 * Handle the mapping result by creating groups/roles and changing the assignment user <-> group and group <-> role
	 * @param tx transaction
	 * @param batch event queue batch
	 * @param result mapping result
	 * @param authUser authenticated user
	 * @param admin admin user
	 * @throws CannotWriteException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void handleMappingResult(Tx tx, EventQueueBatch batch, MappingResult result, HibUser authUser, HibUser admin) throws CannotWriteException {
		RoleDao roleDao = tx.roleDao();
		GroupDao groupDao = tx.groupDao();
		UserDao userDao = tx.userDao();
		HibBaseElement groupRoot = permissionRoots.group();
		HibBaseElement roleRoot = permissionRoots.role();

		List<RoleResponse> mappedRoles = result.getRoles();
		List<GroupResponse> mappedGroups = result.getGroups();

		// Prepare MappingHelper for roles
		MappingHelper<HibRole> rolesHelper = new MappingHelper<>(roleDao);
		rolesHelper.initMapped(mappedRoles, RoleResponse::getUuid, RoleResponse::getName, MappingHelper.Order.UUID_FIRST);
		rolesHelper.initAssigned(mappedGroups, GroupResponse::getRoles, RoleReference::getUuid, RoleReference::getName, MappingHelper.Order.NAME_FIRST);
		rolesHelper.load();
		if (rolesHelper.areMappedEntitiesMissing()) {
			requiresWrite();
		}
		rolesHelper.createMissingMapped(roleName -> {
			log.debug("Creating new role {} via mapping request.", roleName);
			return roleDao.create(roleName, admin);
		}, created -> {
			userDao.inheritRolePermissions(admin, roleRoot, created);
		});

		// Prepare MappingHelper for groups
		MappingHelper<HibGroup> groupsHelper = new MappingHelper<>(groupDao);
		groupsHelper.initMapped(mappedGroups, GroupResponse::getUuid, GroupResponse::getName, MappingHelper.Order.UUID_FIRST);
		groupsHelper.initAssigned(mappedRoles, RoleResponse::getGroups, GroupReference::getUuid, GroupReference::getName, MappingHelper.Order.NAME_FIRST);
		groupsHelper.load();
		if (groupsHelper.areMappedEntitiesMissing()) {
			requiresWrite();
		}
		groupsHelper.createMissingMapped(groupName -> {
			log.debug("Creating group {} via mapping request.", groupName);
			return groupDao.create(groupName, admin);
		}, created -> {
			userDao.inheritRolePermissions(admin, groupRoot, created);
		});

		// check which groups are not yet assigned to the user and add them
		List<HibGroup> assignedGroups = new ArrayList<>(userDao.getGroups(authUser).list());
		List<HibGroup> toAssign = new ArrayList<>(groupsHelper.getMappedEntities());
		toAssign.removeAll(assignedGroups);

		for (HibGroup group : toAssign) {
			requiresWrite();
			log.debug("Adding user {} to group {} via mapping request.", authUser.getUsername(), group.getName());
			userDao.addGroup(authUser, group);
			batch.add(groupDao.createUserAssignmentEvent(group, authUser, ASSIGNED));

			if (!groupsHelper.wasCreated(group)) {
				batch.add(group.onUpdated());
			}
			assignedGroups.add(group);
		}

		// collect information about groups <-> roles assignment
		Map<String, Set<String>> roleUuidsPerGroupUuid = new HashMap<>();
		if (mappedGroups != null) {
			for (GroupResponse mappedGroup : mappedGroups) {
				groupsHelper.getEntity(mappedGroup.getUuid(), mappedGroup.getName()).ifPresent(group -> {
					List<RoleReference> roles = mappedGroup.getRoles();
					if (roles != null) {
						for (RoleReference assignedRole : roles) {
							rolesHelper.getEntity(assignedRole.getUuid(), assignedRole.getName())
									.ifPresent(role -> {
										roleUuidsPerGroupUuid
												.computeIfAbsent(group.getUuid(), k -> new HashSet<>())
												.add(role.getUuid());
									});
						}
					}
				});
			}
		}
		if (mappedRoles != null) {
			for (RoleResponse mappedRole : mappedRoles) {
				rolesHelper.getEntity(mappedRole.getUuid(), mappedRole.getName()).ifPresent(role -> {
					List<GroupReference> groups = mappedRole.getGroups();
					if (groups != null) {
						for (GroupReference assignedGroup : groups) {
							groupsHelper.getEntity(assignedGroup.getUuid(), assignedGroup.getName())
									.ifPresent(group -> {
										roleUuidsPerGroupUuid
												.computeIfAbsent(group.getUuid(), k -> new HashSet<>())
												.add(role.getUuid());
									});
						}
					}
				});
			}
		}

		// batch load the roles currently assigned to groups
		RoleFilter roleFilter = result.getRoleFilter();
		Set<String> groupUuids = new HashSet<>();
		groupUuids.addAll(roleUuidsPerGroupUuid.keySet());
		if (roleFilter != null) {
			groupUuids.addAll(groupsHelper.getMappedEntities().stream().map(HibGroup::getUuid).collect(Collectors.toSet()));
		}
		Map<HibGroup, Collection<? extends HibRole>> rolesPerGroup = groupDao.getRoles(groupsHelper.getEntities(groupUuids));

		// check which group <-> role assignement is not yet present and assign
		for (Entry<String, Set<String>> entry : roleUuidsPerGroupUuid.entrySet()) {
			String groupUuid = entry.getKey();
			Optional<HibGroup> optGroup = groupsHelper.getEntity(groupUuid, null);
			if (optGroup.isPresent()) {
				HibGroup group = optGroup.get();
				List<HibRole> rolesToAssign = new ArrayList<>(rolesHelper.getEntities(entry.getValue()));
				rolesToAssign.removeAll(rolesPerGroup.getOrDefault(group, Collections.emptyList()));

				if (!rolesToAssign.isEmpty()) {
					requiresWrite();
				}

				for (HibRole role : rolesToAssign) {
					groupDao.addRole(group, role);
					batch.add(groupDao.createRoleAssignmentEvent(group, role, ASSIGNED));
					rolesPerGroup.getOrDefault(group, new ArrayList()).add(role);
				}
			}
		}

		// if a role filter is given, remove the filtered group <-> role assignments for the mapped groups
		if (roleFilter != null) {
			for (HibGroup group : groupsHelper.getMappedEntities()) {
				for (HibRole role : rolesPerGroup.getOrDefault(group, Collections.emptyList())) {
					if (roleFilter.filter(group.getName(), role.getName())) {
						requiresWrite();
						log.info("Unassigning role {" + role.getName() + "} from group {" + group.getName() + "}");
						groupDao.removeRole(group, role);
						batch.add(groupDao.createRoleAssignmentEvent(group, role, UNASSIGNED));
					}
				}
			}
		}

		// if a group filter is given, remove the filtered group <-> user assignments
		GroupFilter groupFilter = result.getGroupFilter();
		if (groupFilter != null) {
			for (HibGroup group : assignedGroups) {
				if (groupFilter.filter(group.getName())) {
					requiresWrite();
					log.info("Unassigning group {" + group.getName() + "} from user {" + authUser.getUsername() + "}");
					groupDao.removeUser(group, authUser);
					batch.add(groupDao.createUserAssignmentEvent(group, authUser, UNASSIGNED));
				}
			}
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
