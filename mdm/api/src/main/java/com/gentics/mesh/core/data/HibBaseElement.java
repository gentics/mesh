package com.gentics.mesh.core.data;

import java.util.Set;

import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.error.MissingPermissionException;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Common interface for all base elements.
 */
public interface HibBaseElement extends HibElement {

	/**
	 * Tests if the {@link InternalPermission}s READ_PUBLISHED_PERM and READ_PUBLISHED can be set for this element.
	 * 
	 * @return
	 */
	default boolean hasPublishPermissions() {
		return false;
	}

	/**
	 * @return whether we should check for read permissions before applying any permissions
	 */
	default boolean checkReadPermissionBeforeApplyingPermissions() {
		return true;
	}

	/**
	 * Grant the set of permissions and revoke the other set of permissions to this element using the role.
	 *
	 * @param authUser the user requesting the permissions change
	 * @param batch
	 * @param role
	 * @param recursive
	 * @param permissionsToGrant
	 * @param permissionsToRevoke
	 */
	// TODO This functionality looks too complex for an Entity and should most probably belong to DAO.
	default boolean applyPermissions(MeshAuthUser authUser, EventQueueBatch batch, HibRole role, boolean recursive,
									 Set<InternalPermission> permissionsToGrant, Set<InternalPermission> permissionsToRevoke) {
		RoleDao roleDao = Tx.get().roleDao();
		UserDao userDao = Tx.get().userDao();
		if (checkReadPermissionBeforeApplyingPermissions() && !userDao.hasPermission(authUser.getDelegate(), this, InternalPermission.READ_PERM)) {
			throw new MissingPermissionException(Permission.READ, this.getUuid());
		}

		boolean permissionChanged = false;
		permissionChanged = roleDao.grantPermissions(role, this, permissionsToGrant.toArray(new InternalPermission[permissionsToGrant.size()])) || permissionChanged;
		permissionChanged = roleDao.revokePermissions(role, this, permissionsToRevoke.toArray(new InternalPermission[permissionsToRevoke.size()])) || permissionChanged;

		if (permissionChanged && this instanceof HibCoreElement) {
			HibCoreElement<?> coreVertex = (HibCoreElement<?>) this;
			batch.add(coreVertex.onPermissionChanged(role));
		}
		// TODO Also handle root elements - We need to add a dedicated event in those cases.
		return permissionChanged;
	}
}
