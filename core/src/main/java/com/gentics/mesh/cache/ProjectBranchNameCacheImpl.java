package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_JOINED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.rest.MeshEvent;

@Singleton
public class ProjectBranchNameCacheImpl extends AbstractMeshCache<String, Branch> implements ProjectBranchNameCache {

	private static final long CACHE_SIZE = 500;

	private static final MeshEvent EVENTS[] = {
		BRANCH_UPDATED,
		BRANCH_CREATED,
		BRANCH_DELETED,
		CLUSTER_NODE_JOINED,
		CLUSTER_DATABASE_CHANGE_STATUS,
	};

	@Inject
	public ProjectBranchNameCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry) {
		super(createCache(factory), registry, CACHE_SIZE);
	}

	private static EventAwareCache<String, Branch> createCache(EventAwareCacheFactory factory) {
		return factory.<String, Branch>builder()
			.events(EVENTS)
			.action((event, cache) -> {
				cache.invalidate();
			})
			.maxSize(CACHE_SIZE)
			.name("projectbranchname")
			.build();
	}
}
