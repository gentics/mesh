package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.CLEAR_PERMISSION_STORE;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central LRU permission cache which is used to quickly lookup cached permissions.
 */
@Singleton
public class PermissionCacheImpl implements PermissionCache {

	private static final Logger log = LoggerFactory.getLogger(PermissionCacheImpl.class);

	private final EventAwareCache<String, Boolean> cache;

	private final Vertx vertx;

	private final MeshOptions options;

	@Inject
	public PermissionCacheImpl(Vertx vertx, CacheRegistry registry, MeshOptions options) {
		this.vertx = vertx;
		this.options = options;
		// = Caffeine.newBuilder().maximumSize(100_000).expireAfterWrite(30, TimeUnit.MINUTES).build();
		cache = EventAwareCache.<String, Boolean>builder()
			.events(CLEAR_PERMISSION_STORE)
			.action((event, cache) -> {
				if (log.isDebugEnabled()) {
					log.debug("Clearing permission store due to received event from {" + event.address() + "}");
				}
				cache.invalidate();
			})
			.vertx(vertx)
			.build();

		registry.register(cache);
	}

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
	public boolean hasPermission(Object userId, GraphPermission permission, Object elementId) {
		String key = createCacheKey(userId, permission, elementId);
		Boolean cachedPerm = cache.get(key);
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
	private String createCacheKey(Object userId, GraphPermission permission, Object elementId) {
		return userId + "-" + permission.ordinal() + "-" + elementId;
	}

	/**
	 * Invalidate the LRU cache and optionally notify other instances in the cluster.
	 * 
	 * @param notify
	 */
	@Override
	public void clear(boolean notify) {
		// Invalidate locally
		cache.invalidate();
		if (notify && options.getClusterOptions().isEnabled()) {
			// Send the event to inform other to purge the stored permissions
			vertx.eventBus().publish(CLEAR_PERMISSION_STORE.address, null);
			// log.error("Can't distribute cache clear event. Maybe Vert.x is stopping / starting right now");
		}
	}

	/**
	 * Invalidate the LRU cache.
	 */
	@Override
	public void clear() {
		clear(true);
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
	public void store(Object userId, GraphPermission permission, Object elementId) {
		cache.put(createCacheKey(userId, permission, elementId), true);
	}
}
