package com.gentics.mesh.cache;

import java.util.function.Function;

/**
 * Common interface for caches that are used in Gentics Mesh. 
 *
 * @param <K> Type of the cache key
 * @param <V> Type of the cache value
 */
public interface MeshCache<K, V> {

	/**
	 * Clear the cache.
	 */
	void clear();

	/**
	 * Return the cache entry.
	 * 
	 * @param key
	 *            Cache key
	 * @param mappingFunction
	 *            Mapper that is called to load the entry if it can't be found in the cache
	 * @return Loaded entry or null if no entry could be found
	 */
	V get(K key, Function<K, V> mappingFunction);

	/**
	 * Return the cache entry.
	 * 
	 * @param key
	 *            Cache key
	 * @return Found entry or null if the entry is not cached
	 */
	V get(K key);

	/**
	 * Check whether the cache is disabled.
	 * 
	 * @return
	 */
	boolean isDisabled();

	/**
	 * Enable the cache.
	 */
	void enable();

	/**
	 * Disable the cache.
	 */
	void disable();

	/**
	 * Return the current size of the cache.
	 * 
	 * @return
	 */
	long size();
}
