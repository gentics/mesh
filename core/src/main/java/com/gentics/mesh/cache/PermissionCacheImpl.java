package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.CLEAR_PERMISSION_STORE;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_JOINED;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central LRU permission cache which is used to quickly lookup cached permissions.
 */
@Singleton
public class PermissionCacheImpl extends AbstractMeshCache<String, EnumSet<InternalPermission>> implements PermissionCache {

	private static final Logger log = LoggerFactory.getLogger(PermissionCacheImpl.class);

	private final Vertx vertx;

	private final MeshOptions options;

	private static final long CACHE_SIZE = 100_000;

	private static final MeshEvent EVENTS[] = {
		CLEAR_PERMISSION_STORE,
		CLUSTER_NODE_JOINED,
		CLUSTER_DATABASE_CHANGE_STATUS,
	};

	/**
	 * Map that will contain every used EnumSet (once). This is used for deduplication of the permission EnumSet instances
	 * before putting them into the cache. With 6 permission bits, there are only 2^6=64 possible combinations and we will
	 * keep only unique instances in the cache instead of up to 100_000 different instances.
	 */
	private final Map<EnumSet<InternalPermission>, EnumSet<InternalPermission>> uniqueMap = Collections.synchronizedMap(new HashMap<>());

	@Inject
	public PermissionCacheImpl(EventAwareCacheFactory factory, Vertx vertx, CacheRegistry registry, MeshOptions options) {
		super(createCache(factory), registry, CACHE_SIZE);
		this.vertx = vertx;
		this.options = options;
	}

	private static EventAwareCache<String, EnumSet<InternalPermission>> createCache(EventAwareCacheFactory factory) {
		return factory.<String, EnumSet<InternalPermission>>builder()
			.events(EVENTS)
			.action((event, cache) -> {
				if (log.isDebugEnabled()) {
					log.debug("Clearing permission store due to received event from {" + event.address() + "}");
				}
				cache.invalidate();
			})
			.expireAfter(30, ChronoUnit.MINUTES)
			.maxSize(CACHE_SIZE)
			.name("permission")
			.build();
	}

	@Override
	public Boolean hasPermission(Object userId, InternalPermission permission, Object elementId) {
		String key = createCacheKey(userId, elementId);
		EnumSet<InternalPermission> cachedPermissions = cache.get(key);
		if (cachedPermissions != null) {
			return cachedPermissions.contains(permission);
		} else {
			return null;
		}
	}

	/**
	 * Create the cache key.
	 * 
	 * @param userId
	 * @param permission
	 * @param elementId
	 * @return
	 */
	private String createCacheKey(Object userId, Object elementId) {
		return userId + "-" + elementId;
	}

	/**
	 * Invalidate the LRU cache and optionally notify other instances in the cluster.
	 * 
	 * @param notify
	 *            Whether to publish an event to inform other nodes in the cluster
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

	@Override
	public void store(Object userId, EnumSet<InternalPermission> permission, Object elementId) {
		// deduplicate the permission EnumSet and put it into the cache
		cache.put(createCacheKey(userId, elementId), deduplicate(permission));
	}

	/**
	 * Dedpulicate the permission EnumSet, so that the cache will not contain up to 100_000 different instances
	 * but only up to 2^6=64 different instances.
	 * @param permission permission set
	 * @return deduplicated permission set
	 */
	protected EnumSet<InternalPermission> deduplicate(EnumSet<InternalPermission> permission) {
		// Since the EnumSet is mutable and was passed to the PermissionCache from "outside", we do not
		// put that instance into the cache, but create a clone first.
		EnumSet<InternalPermission> clone = EnumSet.copyOf(permission);

		// either get the already used instance from the map or put the clone in the map, if this is the first occurrance
		return uniqueMap.computeIfAbsent(clone, key -> clone);
	}
}
