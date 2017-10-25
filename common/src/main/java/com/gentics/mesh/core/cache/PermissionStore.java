package com.gentics.mesh.core.cache;

import static com.gentics.mesh.Events.EVENT_CLEAR_PERMISSION_STORE;

import java.util.concurrent.TimeUnit;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central LRU permission cache which is used to quickly lookup cached permissions.
 */
public final class PermissionStore {

	private static final Logger log = LoggerFactory.getLogger(PermissionStore.class);

	public static final Cache<String, Boolean> PERM_CACHE = Caffeine.newBuilder().maximumSize(100_000).expireAfterWrite(30, TimeUnit.MINUTES).build();

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
	 * Register the event handler which can be used to invalidate the LRU cache.
	 */
	public static void registerEventHandler() {
		Mesh.vertx().eventBus().consumer(EVENT_CLEAR_PERMISSION_STORE, e -> {
			if (log.isDebugEnabled()) {
				log.debug("Clearing permission store due to received event from {" + e.address() + "}");
			}
			PERM_CACHE.invalidateAll();
		});
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
	 * Invalidate the LRU cache.
	 */
	public static void invalidate() {
		// Invalidate locally and also send the event to inform other to purge the stored permissions
		PERM_CACHE.invalidateAll();
		Mesh.vertx().eventBus().publish(EVENT_CLEAR_PERMISSION_STORE, null);
	}

	/**
	 * Store a granting permission in the cache.
	 * 
	 * @param userId
	 *            User which currently has roles which grant him the permission on the element
	 * @param permission
	 *            Permission which is granted
	 * @param elementId
	 *            Id of the element to which a permission is granted
	 */
	public static void store(Object userId, GraphPermission permission, Object elementId) {
		PERM_CACHE.put(createCacheKey(userId, permission, elementId), true);
	}
}
