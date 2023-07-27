package com.gentics.mesh.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.MeshEvent;

/**
 * User name cache
 * 
 * @author plyhun
 *
 */
@Singleton
public class UserNameCacheImpl extends AbstractNameCache<HibUser> implements UserNameCache {

	@Inject
	public UserNameCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry) {
		super("username", factory, registry, 1000, new MeshEvent[] {
				MeshEvent.USER_DELETED, MeshEvent.USER_UPDATED
		});
	}
}
