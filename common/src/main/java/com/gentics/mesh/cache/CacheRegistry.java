package com.gentics.mesh.cache;

public interface CacheRegistry {

	/**
	 * Register the given cache.
	 * 
	 * @param cache
	 */
	void register(EventAwareCache<?, ?> cache);

	/**
	 * Clear all registered caches.
	 */
	void clear();

}
