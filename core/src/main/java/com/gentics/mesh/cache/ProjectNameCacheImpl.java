package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_JOINED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.MeshEvent;

@Singleton
public class ProjectNameCacheImpl extends AbstractMeshCache<String, Project> implements ProjectNameCache {

	public static final long CACHE_SIZE = 100;

	private static final MeshEvent EVENTS[] = {
		CLUSTER_NODE_JOINED,
		CLUSTER_DATABASE_CHANGE_STATUS,
		PROJECT_DELETED, 
		PROJECT_UPDATED };

	@Inject
	public ProjectNameCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry) {
		super(createCache(factory), registry, CACHE_SIZE);
	}

	private static EventAwareCache<String, Project> createCache(EventAwareCacheFactory factory) {
		return factory.<String, Project>builder()
			.events(EVENTS)
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
