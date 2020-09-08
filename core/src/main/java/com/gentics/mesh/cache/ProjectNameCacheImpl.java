package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.project.HibProject;

@Singleton
public class ProjectNameCacheImpl extends AbstractMeshCache<String, HibProject> implements ProjectNameCache {

	public static final long CACHE_SIZE = 100;

	@Inject
	public ProjectNameCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry) {
		super(createCache(factory), registry, CACHE_SIZE);
	}

	private static EventAwareCache<String, HibProject> createCache(EventAwareCacheFactory factory) {
		return factory.<String, HibProject>builder()
			.events(PROJECT_DELETED, PROJECT_UPDATED)
			.action((event, cache) -> {
				String name = event.body().getString("name");
				if (name != null) {
					cache.invalidate(name);
				} else {
					cache.invalidate();
				}
			})
			.name("projectname")
			.maxSize(CACHE_SIZE)
			.build();
	}

}
