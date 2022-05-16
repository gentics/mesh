package com.gentics.mesh.cache;

import java.util.EnumSet;

import com.gentics.mesh.core.data.perm.InternalPermission;

/**
 * Permission cache for user perms on elements.
 */
public interface PermissionCache extends MeshCache<String, EnumSet<InternalPermission>> {

	/**
	 * Clear the local cache and send an event to inform other instances to also clear their caches.
	 * 
	 * @param notify
	 */
	void clear(boolean notify);

	/**
	 * Check whether the element with the given id has the permission.
	 * 
	 * @param userId
	 *            User id
	 * @param permission
	 * @param elementId
	 * @return true, if permission is granted, false if it is not granted and null if not cached
	 */
	Boolean hasPermission(Object userId, InternalPermission permission, Object elementId);

	/**
	 * Store the permission in the perm store.
	 * 
	 * @param userId
	 *            User id
	 * @param permissions
	 * @param elementId
	 */
	void store(Object userId, EnumSet<InternalPermission> permissions, Object elementId);

}
