package com.gentics.mesh.core.data;

import java.util.Set;

import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Common interface for all base elements.
 */
public interface HibBaseElement extends HibElement {

	/**
	 * UUID of the element.
	 * 
	 * @return
	 */
	String getUuid();

	/**
	 * Id of the element. This is legacy support method which is used to handle perm checks.
	 * 
	 * @return
	 */
	Object getId();

	/**
	 * Tests if the {@link GraphPermission}s READ_PUBLISHED_PERM and READ_PUBLISHED can be set for this element.
	 * 
	 * @return
	 */
	default boolean hasPublishPermissions() {
		return false;
	}

	/**
	 * Grant the set of permissions and revoke the other set of permissions to this element using the role.
	 * 
	 * @param batch
	 * @param role
	 * @param recursive
	 * @param permissionsToGrant
	 * @param permissionsToRevoke
	 */
	// TODO This functionality looks too complex for an Entity and should most probably belong to DAO.
	default boolean applyPermissions(EventQueueBatch batch, HibRole role, boolean recursive,
			Set<InternalPermission> permissionsToGrant, Set<InternalPermission> permissionsToRevoke) {
		RoleDao roleDao = Tx.get().roleDao();
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
