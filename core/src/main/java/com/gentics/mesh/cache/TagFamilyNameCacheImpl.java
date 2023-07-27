package com.gentics.mesh.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.rest.MeshEvent;

/**
 * Tag family name cache
 * 
 * @author plyhun
 *
 */
@Singleton
public class TagFamilyNameCacheImpl extends AbstractNameCache<HibTagFamily> implements TagFamilyNameCache {

	@Inject
	public TagFamilyNameCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry) {
		super("tagfamilyname", factory, registry, new MeshEvent[] {
				MeshEvent.TAG_FAMILY_DELETED, MeshEvent.TAG_FAMILY_UPDATED
		});
	}
}
