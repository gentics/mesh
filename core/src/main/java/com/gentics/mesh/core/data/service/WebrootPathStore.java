package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.rest.MeshEvent.CLEAR_PATH_STORE;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_MOVED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_PUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNPUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_FINISHED;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.etc.config.CacheConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.path.Path;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central LRU webroot path cache which is used to quickly lookup cached paths.
 */
@Singleton
public class WebrootPathStore {

	private static final Logger log = LoggerFactory.getLogger(WebrootPathStore.class);

	private final Cache<String, Path> pathCache;

	private CacheConfig cacheOptions;

	@Inject
	public WebrootPathStore(MeshOptions options) {
		this.cacheOptions = options.getCacheConfig();
		this.pathCache = Caffeine.newBuilder().maximumSize(cacheOptions.getPathCacheSize()).build();
	}

	/**
	 * Check whether the cache has the path already stored.
	 * 
	 * @param project
	 * @param branch
	 * @param type
	 * @param path
	 * @return Path, if the path could be found in the cache. Otherwise null
	 */
	public Path getPath(Project project, Branch branch, ContainerType type, String path) {
		if (isDisabled()) {
			if (log.isTraceEnabled()) {
				log.trace("Path cache is disabled. Not using cache");
			}
			return null;
		}
		String key = createCacheKey(project, branch, type, path);
		return pathCache.getIfPresent(key);
	}

	/**
	 * Register the event handler which can be used to invalidate the LRU cache.
	 */
	public void registerEventHandler() {
		// No need to register when cache is disabled.
		if (isDisabled()) {
			return;
		}
		EventBus eb = Mesh.vertx().eventBus();

		Arrays.asList(CLEAR_PATH_STORE,
			NODE_UPDATED,
			NODE_DELETED,
			NODE_PUBLISHED,
			NODE_UNPUBLISHED,
			NODE_MOVED,
			NODE_CONTENT_CREATED,
			NODE_CONTENT_DELETED,
			SCHEMA_MIGRATION_FINISHED)
			.forEach(event -> {
				eb.consumer(event.address, e -> {
					invalidateByEvent(e);
				});
			});

	}

	private void invalidateByEvent(Message<Object> e) {
		if (log.isDebugEnabled()) {
			log.debug("Clearing path store due to received event from {" + e.address() + "}");
		}
		pathCache.invalidateAll();
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

	/**
	 * Store a path in the cache.
	 * 
	 * @param project
	 *            Project for which the path is valid
	 * @param branch
	 *            Used branch
	 * @param type
	 *            Type of the resolved content
	 * @param path
	 *            Webroot path
	 * @param resolvedPath
	 *            Resolved webroot path to be put in the cache
	 */
	public void store(Project project, Branch branch, ContainerType type, String path, Path resolvedPath) {
		if (isDisabled()) {
			return;
		}
		pathCache.put(createCacheKey(project, branch, type, path), resolvedPath);
	}

	public boolean isDisabled() {
		return cacheOptions.getPathCacheSize() == 0;
	}

	/**
	 * Invalidate the cache.
	 */
	public void invalidate() {
		pathCache.invalidateAll();
	}
}
