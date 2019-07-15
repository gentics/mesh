package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.rest.MeshEvent.CLEAR_PATH_STORE;
import static com.gentics.mesh.core.rest.MeshEvent.CLEAR_PERMISSION_STORE;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_MOVED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_PUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNPUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_FINISHED;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.path.Path;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central LRU webroot path cache which is used to quickly lookup cached paths.
 */
public final class WebrootPathStore {

	private static final Logger log = LoggerFactory.getLogger(WebrootPathStore.class);

	public static final Cache<String, Path> PATH_CACHE = Caffeine.newBuilder().maximumSize(50_000).build();

	/**
	 * Check whether the cache has the path already stored.
	 * 
	 * @param userId
	 *            Vertex id of the user
	 * @param permission
	 *            Permission to check against
	 * @param path
	 *            Webroot path
	 * @return Path, if the path could be found in the cache. Otherwise null
	 */
	public static Path getPath(Project project, Branch branch, ContainerType type, String path) {
		String key = createCacheKey(project, branch, type, path);
		return PATH_CACHE.getIfPresent(key);
	}

	/**
	 * Register the event handler which can be used to invalidate the LRU cache.
	 */
	public static void registerEventHandler() {
		EventBus eb = Mesh.vertx().eventBus();

		Arrays.asList(CLEAR_PERMISSION_STORE,
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

	private static void invalidateByEvent(Message<Object> e) {
		if (log.isDebugEnabled()) {
			log.debug("Clearing permission store due to received event from {" + e.address() + "}");
		}
		PATH_CACHE.invalidateAll();
	}

	/**
	 * Create the cache key.
	 * 
	 * @param project
	 * @param elementId
	 * @return
	 */
	private static String createCacheKey(Project project, Branch branch, ContainerType type, String path) {
		return project.id() + "-" + branch.id() + "-" + type.getCode() + "-" + path;
	}

	/**
	 * Invalidate the LRU cache and optionally notify other instances in the cluster.
	 * 
	 * @param notify
	 */
	public static void invalidate(boolean notify) {
		// Invalidate locally
		PATH_CACHE.invalidateAll();
		if (notify) {
			// Send the event to inform other to purge the stored permissions
			Vertx vertx = Mesh.vertx();
			if (vertx != null) {
				vertx.eventBus().publish(CLEAR_PATH_STORE.address, null);
			} else {
				log.error("Can't distribute path cache clear event. Maybe Vert.x is stopping / starting right now");
			}
		}
	}

	/**
	 * Invalidate the LRU cache.
	 */
	public static void invalidate() {
		invalidate(true);
	}

	/**
	 * Store a granting permission in the cache.
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
	public static void store(Project project, Branch branch, ContainerType type, String path, Path resolvedPath) {
		PATH_CACHE.put(createCacheKey(project, branch, type, path), resolvedPath);
	}
}
