package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_UPDATED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.Branch;

import io.vertx.core.Vertx;

@Singleton
public class ProjectBranchNameCacheImpl extends AbstractMeshCache<String, Branch> implements ProjectBranchNameCache {

	private static final long CACHE_SIZE = 500;

	@Inject
	public ProjectBranchNameCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry) {
		super(createCache(factory), registry, CACHE_SIZE);
	}

	private static EventAwareCache<String, Branch> createCache(EventAwareCacheFactory factory) {
		return factory.<String, Branch>builder()
			.events(BRANCH_UPDATED, BRANCH_CREATED, BRANCH_DELETED)
			.action((event, cache) -> {
				cache.invalidate();
			})
			.maxSize(CACHE_SIZE)
			.name("projectbranchname")
			.build();
	}
}
