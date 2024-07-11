package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_JOINED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.rest.MeshEvent;

/**
 * @see GroupNameCache
 * 
 * @author plyhun
 *
 */
@Singleton
public class GroupNameCacheImpl extends AbstractNameCache<Group> implements GroupNameCache {

	@Inject
	public GroupNameCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry) {
		super("groupname", factory, registry, new MeshEvent[] {
				CLUSTER_NODE_JOINED, 
				CLUSTER_DATABASE_CHANGE_STATUS, 
				GROUP_DELETED,
				GROUP_UPDATED,
				GROUP_CREATED
			});
	}
}
