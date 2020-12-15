package com.gentics.mesh.cache;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @see CacheRegistry
 */
@Singleton
public class CacheRegistryImpl implements CacheRegistry {

	private Set<EventAwareCache<?, ?>> caches = new HashSet<>();

	@Inject
	public CacheRegistryImpl() {
	}

	@Override
	public void register(EventAwareCache<?, ?> cache) {
		caches.add(cache);
	}

	@Override
	public void clear() {
		caches.forEach(EventAwareCache::invalidate);
	}

}
