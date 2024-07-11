package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_JOINED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_UPDATED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.rest.MeshEvent;

/**
 * Tag named cache.
 * 
 * @author plyhun
 *
 */
@Singleton
public class TagNameCacheImpl extends AbstractNameCache<Tag> implements TagNameCache {

	@Inject
	public TagNameCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry) {
		super("tagname", factory, registry, new MeshEvent[] {
				CLUSTER_NODE_JOINED, CLUSTER_DATABASE_CHANGE_STATUS, TAG_DELETED, TAG_UPDATED, TAG_FAMILY_DELETED, TAG_FAMILY_UPDATED, TAG_FAMILY_CREATED, TAG_CREATED
		});
	}
}
