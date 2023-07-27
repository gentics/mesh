package com.gentics.mesh.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.MeshEvent;

/**
 * Role named cache
 * 
 * @author plyhun
 *
 */
@Singleton
public class RoleNameCacheImpl extends AbstractNameCache<HibRole> implements RoleNameCache {

	@Inject
	public RoleNameCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry) {
		super("rolename", factory, registry, new MeshEvent[] {
				MeshEvent.ROLE_UPDATED, MeshEvent.ROLE_DELETED
		});
	}
}
