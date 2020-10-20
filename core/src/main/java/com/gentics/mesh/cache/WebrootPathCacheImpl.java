package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.CLEAR_PATH_STORE;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_JOINED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_MOVED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_PUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNPUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_FINISHED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.etc.config.CacheConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.path.Path;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central LRU webroot path cache which is used to quickly lookup cached paths.
 */
@Singleton
public class WebrootPathCacheImpl extends AbstractMeshCache<String, Path> implements WebrootPathCache {

	private static final Logger log = LoggerFactory.getLogger(WebrootPathCacheImpl.class);

	private static final MeshEvent EVENTS[] = {
		CLEAR_PATH_STORE,
		NODE_UPDATED,
		NODE_DELETED,
		NODE_PUBLISHED,
		NODE_UNPUBLISHED,
		NODE_MOVED,
		NODE_CONTENT_CREATED,
		NODE_CONTENT_DELETED,
		CLUSTER_NODE_JOINED,
		CLUSTER_DATABASE_CHANGE_STATUS,
		SCHEMA_MIGRATION_FINISHED };

	@Inject
	public WebrootPathCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry, MeshOptions options) {
		super(createCache(factory, options.getCacheConfig()), registry, options.getCacheConfig().getPathCacheSize());
	}

	private static EventAwareCache<String, Path> createCache(EventAwareCacheFactory factory, CacheConfig config) {
		return factory.<String, Path>builder()
			.events(EVENTS)
			.action((event, cache) -> {
				if (log.isDebugEnabled()) {
					log.debug("Clearing path store due to received event from {" + event.address() + "}");
				}
				cache.invalidate();
			})
			.name("webroot")
			.maxSize(config.getPathCacheSize())
			.build();
	}

	@Override
	public Path getPath(Project project, Branch branch, ContainerType type, String path) {
		if (isDisabled()) {
			if (log.isTraceEnabled()) {
				log.trace("Path cache is disabled. Not using cache");
			}
			return null;
		}
		String key = createCacheKey(project, branch, type, path);
		Path value = cache.get(key);
		if (value == null || !value.isValid()) {
			return null;
		} else {
			return value;
		}
	}

	@Override
	public void store(Project project, Branch branch, ContainerType type, String path, Path resolvedPath) {
		if (isDisabled()) {
			return;
		}
		cache.put(createCacheKey(project, branch, type, path), resolvedPath);
	}

	/**
	 * Create the cache key.
	 * 
	 * @param project
	 * @param elementId
	 * @return
	 */
	private String createCacheKey(Project project, Branch branch, ContainerType type, String path) {
		return project.id() + "-" + branch.id() + "-" + type.getCode() + "-" + path;
	}

}
