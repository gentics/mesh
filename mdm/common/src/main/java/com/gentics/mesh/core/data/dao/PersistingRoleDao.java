package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
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
public interface PersistingRoleDao extends RoleDao, PersistingDaoGlobal<HibRole> {

    /**
     * Revoke role permission. Consumers implementing this method do not need to invalidate the cache
     * @param role the role
     * @param element
     * @param permissions
     */
    boolean revokeRolePermissions(HibRole role, HibBaseElement element, InternalPermission... permissions);

    /**
     * Create a new role
     *
     * @param ac
     * @param batch
     * @param uuid
     *            Uuid of the role
     * @return
     */
    default HibRole create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
        RoleCreateRequest requestModel = ac.fromJson(RoleCreateRequest.class);
        String roleName = requestModel.getName();
        UserDao userDao = Tx.get().userDao();
        HibBaseElement roleRoot = Tx.get().data().permissionRoots().role();

        HibUser requestUser = ac.getUser();
        if (StringUtils.isEmpty(roleName)) {
            throw error(BAD_REQUEST, "error_name_must_be_set");
        }

        HibRole conflictingRole = findByName(roleName);
        if (conflictingRole != null) {
            throw conflict(conflictingRole.getUuid(), roleName, "role_conflicting_name");
        }

        if (!userDao.hasPermission(requestUser, roleRoot, CREATE_PERM)) {
            throw error(FORBIDDEN, "error_missing_perm", roleRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
        }

        HibRole role = create(requestModel.getName(), requestUser, uuid);
        userDao.inheritRolePermissions(requestUser, roleRoot, role);
        batch.add(role.onCreated());
        return role;
    }

    /**
     * Revoke the given permissions and clear the cache when successful
     *
     * @param role
     * @param element
     * @param permissions
     * @return
     */
    default boolean revokePermissions(HibRole role, HibBaseElement element, InternalPermission... permissions) {
        boolean permissionsRevoked = revokeRolePermissions(role, element, permissions);
        if (permissionsRevoked) {
            PermissionCache cache = Tx.get().permissionCache();
            cache.clear();
        }
        return permissionsRevoked;
    }

    @Override
    default void delete(HibRole role, BulkActionContext bac) {
        bac.add(role.onDeleted());
        deletePersisted(role);
        bac.process();
        PermissionCache permissionCache = Tx.get().permissionCache();

        permissionCache.clear();
    }

    @Override
	default Result<? extends HibRole> getRolesWithPerm(HibBaseElement element, InternalPermission perm) {
		Set<String> roleUuids = getRoleUuidsForPerm(element, perm);
		Stream<String> stream = roleUuids == null
			? Stream.empty()
			: roleUuids.stream();
		return new TraversalResult<>(stream
			.map(this::findByUuid)
			.filter(Objects::nonNull));
	}

    @Override
	default PermissionInfo getRolePermissions(HibBaseElement element, InternalActionContext ac, String roleUuid) {
		if (!isEmpty(roleUuid)) {
			HibRole role = loadObjectByUuid(ac, roleUuid, READ_PERM);
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
	default void setRolePermissions(HibBaseElement element, InternalActionContext ac, GenericRestResponse model) {
		model.setRolePerms(getRolePermissions(element, ac, ac.getRolePermissionParameters().getRoleUuid()));
	}

	@Override
	default void applyPermissions(HibBaseElement element, EventQueueBatch batch, HibRole role, boolean recursive,
		Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke) {
		element.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	default HibRole create(String name, HibUser creator, String uuid) {
		HibRole role = createPersisted(uuid);
		role.setName(name);
		role.setCreated(creator);
		role.generateBucketId();
		addRole(role);
		return role;
	}

	@Override
	default boolean update(HibRole role, InternalActionContext ac, EventQueueBatch batch) {
		RoleUpdateRequest requestModel = ac.fromJson(RoleUpdateRequest.class);
		if (shouldUpdate(requestModel.getName(), role.getName())) {
			// Check for conflict
			HibRole roleWithSameName = findByName(requestModel.getName());
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
	default boolean hasPermission(HibRole role, InternalPermission permission, HibBaseElement vertex) {
		Set<String> allowedUuids = getRoleUuidsForPerm(vertex, permission);
		return allowedUuids != null && allowedUuids.contains(role.getUuid());
	}

	@Override
	default Set<InternalPermission> getPermissions(HibRole role, HibBaseElement element) {
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
	default RoleResponse transformToRestSync(HibRole role, InternalActionContext ac, int level, String... languageTags) {
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

	private void setGroups(HibRole role, InternalActionContext ac, RoleResponse restRole) {
		for (HibGroup group : role.getGroups()) {
			restRole.getGroups().add(group.transformToReference());
		}
	}

}
