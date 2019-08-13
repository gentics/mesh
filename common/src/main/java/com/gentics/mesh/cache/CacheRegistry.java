package com.gentics.mesh.cache;

/**
 * The cache registry is used to manage all caches at a single location.
 */
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
