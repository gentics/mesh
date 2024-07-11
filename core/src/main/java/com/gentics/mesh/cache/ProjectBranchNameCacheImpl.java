package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_JOINED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_LATEST_BRANCH_UPDATED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.rest.MeshEvent;

/**
 * @see ProjectBranchNameCache
 */
@Singleton
public class ProjectBranchNameCacheImpl extends AbstractNameCache<Branch> implements ProjectBranchNameCache {

	private static final long CACHE_SIZE = 500;

	@Inject
	public ProjectBranchNameCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry) {
		super("projectbranchname", factory, registry, CACHE_SIZE, new MeshEvent[] {
				BRANCH_UPDATED,
				BRANCH_CREATED,
				BRANCH_DELETED,
				CLUSTER_NODE_JOINED,
				CLUSTER_DATABASE_CHANGE_STATUS,
				PROJECT_LATEST_BRANCH_UPDATED
			});
	}
}
