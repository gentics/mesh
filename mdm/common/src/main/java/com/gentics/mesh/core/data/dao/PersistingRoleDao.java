package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import org.apache.commons.lang3.StringUtils;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

/**
 * A persisting extension to {@link RoleDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingRoleDao extends RoleDao, PersistingDaoGlobal<HibRole> {

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

    /**
     * Revoke role permission. Consumers implementing this method do not need to invalidate the cache
     * @param role the role
     * @param element
     * @param permissions
     */
    boolean revokeRolePermissions(HibRole role, HibBaseElement element, InternalPermission... permissions);

    @Override
    default void delete(HibRole role, BulkActionContext bac) {
        bac.add(role.onDeleted());
        deletePersisted(role);
        bac.process();
        PermissionCache permissionCache = Tx.get().permissionCache();

        permissionCache.clear();
    }
}
