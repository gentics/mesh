package com.gentics.mesh.cache;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CacheRegistry {

	private Set<EventAwareCache<?, ?>> caches = new HashSet<>();

	@Inject
	public CacheRegistry() {
	}

	public void register(EventAwareCache<?, ?> cache) {
		this.caches.add(cache);
	}

	public void clear() {
		caches.forEach(EventAwareCache::invalidate);
	}

}
