package com.gentics.mesh.core.cache;

import java.util.concurrent.TimeUnit;

import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Central LRU permission cache which is used to quickly lookup cached permissions.
 */
public final class PermissionStore {

	public static final Cache<String, Boolean> PERM_CACHE = Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(5, TimeUnit.MINUTES).build();

	/**
	 * Check whether the granting user permission was stored in the cache.
	 * 
	 * @param userId
	 *            Vertex id of the user
	 * @param permission
	 *            Permission to check against
	 * @param elementId
	 *            Vertex id of the element to which permissions should be checked
	 * @return true, if a granting permission was found or false if the permission could not be found in the cache
	 */
	public static boolean hasPermission(Object userId, GraphPermission permission, Object elementId) {
		String key = createCacheKey(userId, permission, elementId);
		Boolean cachedPerm = PERM_CACHE.getIfPresent(key);
		return cachedPerm != null && cachedPerm;
	}

	/**
	 * Create the cache key.
	 * 
	 * @param userId
	 * @param permission
	 * @param elementId
	 * @return
	 */
	private static String createCacheKey(Object userId, GraphPermission permission, Object elementId) {
		return userId + "-" + permission.ordinal() + "-" + elementId;
	}

	/**
	 * Invalidate the LRU cache
	 */
	public static void invalidate() {
		PERM_CACHE.invalidateAll();
	}

	/**
	 * Store a granting permission in the cache.
	 * 
	 * @param userId
	 * @param permission
	 * @param elementId
	 */
	public static void store(Object userId, GraphPermission permission, Object elementId) {
		PERM_CACHE.put(createCacheKey(userId, permission, elementId), true);
	}
}
