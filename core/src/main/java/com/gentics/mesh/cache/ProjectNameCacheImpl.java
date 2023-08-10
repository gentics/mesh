package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_JOINED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.rest.MeshEvent;

/**
 * @see ProjectNameCache
 */
@Singleton
public class ProjectNameCacheImpl extends AbstractNameCache<HibProject> implements ProjectNameCache {

	@Inject
	public ProjectNameCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry) {
		super("projectname", factory, registry, new MeshEvent[] {
			CLUSTER_NODE_JOINED,
			CLUSTER_DATABASE_CHANGE_STATUS,
			PROJECT_DELETED, 
			PROJECT_UPDATED 
		});
	}
}
