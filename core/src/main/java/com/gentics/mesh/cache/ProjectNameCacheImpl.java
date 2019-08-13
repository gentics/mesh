package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Project;

import io.vertx.core.Vertx;

@Singleton
public class ProjectNameCacheImpl extends AbstractMeshCache<String, Project> implements ProjectNameCache {

	public static final long CACHE_SIZE = 100;

	@Inject
	public ProjectNameCacheImpl(Vertx vertx, CacheRegistry registry) {
		super(createCache(vertx), registry, CACHE_SIZE);
	}

	private static EventAwareCache<String, Project> createCache(Vertx vertx) {
		return EventAwareCache.<String, Project>builder()
			.events(PROJECT_DELETED, PROJECT_UPDATED)
			.action((event, cache) -> {
				String name = event.body().getString("name");
				if (name != null) {
					cache.invalidate(name);
				} else {
					cache.invalidate();
				}
			})
			.maxSize(CACHE_SIZE)
			.vertx(vertx)
			.build();
	}

}
