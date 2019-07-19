package com.gentics.mesh.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.rest.MeshEvent;

import io.vertx.core.Vertx;

@Singleton
public class ProjectBranchNameCacheImpl implements ProjectBranchNameCache {

	private final EventAwareCache<String, Branch> cache;

	@Inject
	public ProjectBranchNameCacheImpl(Vertx vertx, CacheRegistry registry) {

		/**
		 * Cache for project specific branches.
		 */
		cache = EventAwareCache.<String, Branch>builder()
			.size(500)
			.events(MeshEvent.BRANCH_UPDATED)
			.vertx(vertx)
			.build();

		registry.register(cache);

	}

	@Override
	public EventAwareCache<String, Branch> cache() {
		return cache;
	}
}
