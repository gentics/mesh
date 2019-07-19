package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_UPDATED;

import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Branch;

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
			.events(BRANCH_UPDATED, BRANCH_CREATED)
			.vertx(vertx)
			.build();

		registry.register(cache);

	}

	@Override
	public void clear() {
		cache.invalidate();
	}

	@Override
	public Branch get(String key, Function<String, Branch> mappingFunction) {
		return cache.get(key, mappingFunction);
	}

	public EventAwareCache<String, Branch> cache() {
		return cache;
	}
}
