package com.gentics.mesh.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.rest.MeshEvent;

/**
 * Tag named cache.
 * 
 * @author plyhun
 *
 */
@Singleton
public class TagNameCacheImpl extends AbstractNameCache<HibTag> implements TagNameCache {

	@Inject
	public TagNameCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry, long maxSize,
			MeshEvent[] events) {
		super("tagname", factory, registry, new MeshEvent[] {
			MeshEvent.TAG_DELETED, MeshEvent.TAG_UPDATED, MeshEvent.TAG_FAMILY_DELETED, MeshEvent.TAG_FAMILY_UPDATED
		});
	}
}
