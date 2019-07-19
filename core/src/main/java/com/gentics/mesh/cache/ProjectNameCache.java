package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Project;

import io.vertx.core.Vertx;

@Singleton
public class ProjectNameCache {
	public EventAwareCache<String, Project> cache;

	@Inject
	public ProjectNameCache(Vertx vertx, CacheRegistry registry) {
		cache = EventAwareCache.<String, Project>builder()
			.events(PROJECT_DELETED, PROJECT_UPDATED)
			.action((event, cache) -> {
				String name = event.body().getString("name");
				if (name != null) {
					cache.invalidate(name);
				} else {
					cache.invalidate();
				}
			})
			.vertx(vertx)
			.build();

		registry.register(cache);
	}

	public EventAwareCache<String, Project> cache() {
		return cache;
	}
}
