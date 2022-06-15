package com.gentics.mesh.cache;

import java.util.HashSet;
import java.util.Set;

/**
 * @see CacheRegistry
 */
public abstract class AbstractCacheRegistry implements CacheRegistry {

	private Set<EventAwareCache<?, ?>> caches = new HashSet<>();

	@Override
	public void register(EventAwareCache<?, ?> cache) {
		caches.add(cache);
	}

	@Override
	public void clear() {
		caches.forEach(EventAwareCache::invalidate);
	}

}
