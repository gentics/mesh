package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cache.NameCache;
import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

/**
 * A persisting extension to {@link RoleDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingRoleDao extends RoleDao, PersistingDaoGlobal<Role>, PersistingNamedEntityDao<Role> {
	/**
	 * Grant role permission. Consumers implementing this method do not need to invalidate the cache
	 * @param role the role
	 * @param element
	 * @param permissions
	 */
	boolean grantRolePermissions(Role role, BaseElement element, InternalPermission... permissions);

	/**
	 * Grant the given permissions on the element to the set of roles. Implementations do not need to invalidate the cache
	 *
	 * @param roles set of roles
	 * @param element element to grant permission on
	 * @param exclusive true to revoke the given permissions on all other roles
	 * @param permissions permissions to grant
	 * @return true, iff permissions were effectively changed
	 */
	boolean grantRolePermissions(Set<Role> roles, BaseElement element, boolean exclusive,
			InternalPermission... permissions);

	/**
	 * Grant the given permissions on the element to the set of roles, identified by their uuids. Implementations do not need to invalidate the cache
	 *
	 * @param roleUuids set of role uuids
	 * @param element element to grant permission on
	 * @param exclusive true to revoke the given permissions on all other roles
	 * @param permissions permissions to grant
	 * @return true, iff permissions were effectively changed
	 */
	boolean grantRolePermissionsWithUuids(Set<String> roleUuids, BaseElement element, boolean exclusive,
			InternalPermission... permissions);

	/**
	 * Revoke role permission. Consumers implementing this method do not need to invalidate the cache
	 * @param role the role
	 * @param element
	 * @param permissions
	 */
	boolean revokeRolePermissions(Role role, BaseElement element, InternalPermission... permissions);

	/**
	 * Revoke role permission. Consumers implementing this method do not need to invalidate the cache
	 * @param roles set of roles
	 * @param element element to revoke permissions from
	 * @param permissions permissions to revoke
	 * @return true, iff permissions were effectively changed
	 */
	boolean revokeRolePermissions(Set<Role> roles, BaseElement element, InternalPermission... permissions);

	/**
	 * Revoke role permission. Consumers implementing this method do not need to invalidate the cache
	 * @param roleUuids set of role uuids
	 * @param element element to revoke permissions from
	 * @param permissions permissions to revoke
	 * @return true, iff permissions were effectively changed
	 */
	boolean revokeRolePermissionsWithUuids(Set<String> roleUuids, BaseElement element, InternalPermission... permissions);

	/**
	 * Create a new role
	 *
	 * @param ac
	 * @param batch
	 * @param uuid
	 *			Uuid of the role
	 * @return
	 */
	default Role create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		RoleCreateRequest requestModel = ac.fromJson(RoleCreateRequest.class);
		String roleName = requestModel.getName();
		UserDao userDao = Tx.get().userDao();
		BaseElement roleRoot = Tx.get().data().permissionRoots().role();

		User requestUser = ac.getUser();
		if (StringUtils.isEmpty(roleName)) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}

		Role conflictingRole = findByName(roleName);
		if (conflictingRole != null) {
			throw conflict(conflictingRole.getUuid(), roleName, "role_conflicting_name");
		}

		if (!userDao.hasPermission(requestUser, roleRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", roleRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}

		Role role = create(requestModel.getName(), requestUser, uuid);
		userDao.inheritRolePermissions(requestUser, roleRoot, role);
		return role;
	}

	@Override
	default boolean grantPermissions(Role role, BaseElement element, InternalPermission... permissions) {
		boolean permissionsGranted = grantRolePermissions(role, element, permissions);
		if (permissionsGranted) {
			PermissionCache cache = Tx.get().permissionCache();
			cache.clear();
		}
		return permissionsGranted;
	}

	@Override
	default boolean grantPermissions(Set<Role> roles, BaseElement element, boolean exclusive,
			InternalPermission... permissions) {
		boolean permissionsGranted = grantRolePermissions(roles, element, exclusive, permissions);
		if (permissionsGranted) {
			PermissionCache cache = Tx.get().permissionCache();
			cache.clear();
		}
		return permissionsGranted;
	}

	@Override
	default boolean grantPermissionsWithUuids(Set<String> roleUuids, BaseElement element, boolean exclusive,
			InternalPermission... permissions) {
		boolean permissionsGranted = grantRolePermissionsWithUuids(roleUuids, element, exclusive, permissions);
		if (permissionsGranted) {
			PermissionCache cache = Tx.get().permissionCache();
			cache.clear();
		}
		return permissionsGranted;
	}

	/**
	 * Revoke the given permissions and clear the cache when successful
	 *
	 * @param role
	 * @param element
	 * @param permissions
	 * @return
	 */
	default boolean revokePermissions(Role role, BaseElement element, InternalPermission... permissions) {
		boolean permissionsRevoked = revokeRolePermissions(role, element, permissions);
		if (permissionsRevoked) {
			PermissionCache cache = Tx.get().permissionCache();
			cache.clear();
		}
		return permissionsRevoked;
	}

	@Override
	default boolean revokePermissions(Set<Role> roles, BaseElement element, InternalPermission... permissions) {
		boolean permissionsRevoked = revokeRolePermissions(roles, element, permissions);
		if (permissionsRevoked) {
			PermissionCache cache = Tx.get().permissionCache();
			cache.clear();
		}
		return permissionsRevoked;
	}

	@Override
	default boolean revokePermissionsWithUuids(Set<String> roleUuids, BaseElement element,
			InternalPermission... permissions) {
		boolean permissionsRevoked = revokeRolePermissionsWithUuids(roleUuids, element, permissions);
		if (permissionsRevoked) {
			PermissionCache cache = Tx.get().permissionCache();
			cache.clear();
		}
		return permissionsRevoked;
	}

	@Override
	default void delete(Role role, BulkActionContext bac) {
		bac.add(role.onDeleted());
		deletePersisted(role);
		bac.process();
		PermissionCache permissionCache = Tx.get().permissionCache();

		permissionCache.clear();
	}

	@Override
	default Result<? extends Role> getRolesWithPerm(BaseElement element, InternalPermission perm) {
		Set<String> roleUuids = getRoleUuidsForPerm(element, perm);
		Stream<String> stream = roleUuids == null
			? Stream.empty()
			: roleUuids.stream();
		return new TraversalResult<>(stream
			.map(this::findByUuid)
			.filter(Objects::nonNull));
	}

	@Override
	default PermissionInfo getRolePermissions(BaseElement element, InternalActionContext ac, String roleUuid) {
		if (!isEmpty(roleUuid)) {
			Role role = loadObjectByUuid(ac, roleUuid, READ_PERM);
			if (role != null) {
				PermissionInfo permissionInfo = new PermissionInfo();
				Set<InternalPermission> permSet = getPermissions(role, element);
				for (InternalPermission permission : permSet) {
					permissionInfo.set(permission.getRestPerm(), true);
				}
				permissionInfo.setOthers(false);
				return permissionInfo;
			}
		}
		return null;
	}

	@Override
	default void setRolePermissions(BaseElement element, InternalActionContext ac, GenericRestResponse model) {
		model.setRolePerms(getRolePermissions(element, ac, ac.getRolePermissionParameters().getRoleUuid()));
	}

	@Override
	default void applyPermissions(MeshAuthUser authUser, BaseElement element, EventQueueBatch batch, Role role, boolean recursive,
								  Set<InternalPermission> permissionsToGrant,
								  Set<InternalPermission> permissionsToRevoke) {
		element.applyPermissions(authUser, batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	default Role create(String name, User creator, String uuid) {
		Role role = createPersisted(uuid, r -> {
			r.setName(name);
			r.setCreated(creator);
		});
		role.generateBucketId();
		addRole(role);
		addBatchEvent(role.onCreated());
		uncacheSync(role);
		return mergeIntoPersisted(role);
	}

	@Override
	default boolean update(Role role, InternalActionContext ac, EventQueueBatch batch) {
		RoleUpdateRequest requestModel = ac.fromJson(RoleUpdateRequest.class);
		if (shouldUpdate(requestModel.getName(), role.getName())) {
			// Check for conflict
			Role roleWithSameName = findByName(requestModel.getName());
			if (roleWithSameName != null && !roleWithSameName.getUuid().equals(role.getUuid())) {
				throw conflict(roleWithSameName.getUuid(), requestModel.getName(), "role_conflicting_name");
			}

			role.setName(requestModel.getName());
			batch.add(role.onUpdated());
			return true;
		}
		return false;
	}

	@Override
	default boolean hasPermission(Role role, InternalPermission permission, BaseElement vertex) {
		Set<String> allowedUuids = getRoleUuidsForPerm(vertex, permission);
		return allowedUuids != null && allowedUuids.contains(role.getUuid());
	}

	@Override
	default Set<InternalPermission> getPermissions(Role role, BaseElement element) {
		Set<InternalPermission> permissions = new HashSet<>();
		InternalPermission[] possiblePermissions = element.hasPublishPermissions()
			? InternalPermission.values()
			: InternalPermission.basicPermissions();

		for (InternalPermission permission : possiblePermissions) {
			if (hasPermission(role, permission, element)) {
				permissions.add(permission);
			}
		}
		return permissions;
	}

	@Override
	default Map<Role, Set<InternalPermission>> getPermissions(Set<Role> roles, BaseElement element) {
		if (CollectionUtils.isEmpty(roles)) {
			return Collections.emptyMap();
		}
		Map<Role, Set<InternalPermission>> permissionsMap = new HashMap<>();
		InternalPermission[] possiblePermissions = element.hasPublishPermissions()
				? InternalPermission.values()
				: InternalPermission.basicPermissions();

		for (InternalPermission permission : possiblePermissions) {
			Set<String> allowedUuids = getRoleUuidsForPerm(element, permission);
			for (Role role : roles) {
				Set<InternalPermission> permissions = permissionsMap.computeIfAbsent(role, key -> new HashSet<>());

				if (allowedUuids != null && allowedUuids.contains(role.getUuid())) {
					permissions.add(permission);
				}
			}
		}

		return permissionsMap;
	}

	@Override
	default RoleResponse transformToRestSync(Role role, InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		RoleResponse restRole = new RoleResponse();

		if (fields.has("name")) {
			restRole.setName(role.getName());
		}

		if (fields.has("groups")) {
			setGroups(role, ac, restRole);
		}
		role.fillCommonRestFields(ac, fields, restRole);

		setRolePermissions(role, ac, restRole);
		return restRole;
	}

	@Override
	default Optional<NameCache<Role>> maybeGetCache() {
		return Tx.maybeGet().map(CommonTx.class::cast).map(tx -> tx.data().mesh().roleNameCache());
	}

	private void setGroups(Role role, InternalActionContext ac, RoleResponse restRole) {
		for (Group group : role.getGroups()) {
			restRole.getGroups().add(group.transformToReference());
		}
	}
}
