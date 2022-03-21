package com.gentics.mesh.cache;

import javax.inject.Inject;

/**
 * General implementation of {@link CacheCollection}.
 * 
 * @author plyhun
 *
 */
public class CacheCollectionImpl implements CacheCollection {
	
	private final PermissionCache permissionCache;

	@Inject
	public CacheCollectionImpl(PermissionCache permissionCache) {
		this.permissionCache = permissionCache;
	}

	@Override
	public PermissionCache permissionCache() {
		return permissionCache;
	}
}
