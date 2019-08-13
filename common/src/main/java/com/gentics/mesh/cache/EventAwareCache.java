package com.gentics.mesh.cache;

import java.util.function.Function;

import javax.annotation.Nonnull;

import com.gentics.mesh.cache.impl.EventAwareCacheImpl;

/**
 * An event aware cache is a cache which will be invalidated when an event gets received.
 * 
 * @param <K>
 * @param <V>
 */
public interface EventAwareCache<K, V> {

	/**
	 * Return a builder for a new cache.
	 * 
	 * @param <K>
	 * @param <V>
	 * @return
	 */
	static <K, V> EventAwareCacheImpl.Builder<K, V> builder() {
		return EventAwareCacheImpl.builder();
	}

	/**
	 * Invalidate the cache.
	 */
	void invalidate();

	/**
	 * Invalidate the cache entry with the key.
	 * 
	 * @param key
	 */
	void invalidate(K key);

	/**
	 * Add the given entry to the cache.
	 * 
	 * @param key
	 * @param value
	 */
	void put(K key, V value);

	/**
	 * Load the value from the cache.
	 * 
	 * @param key
	 * @return
	 */
	V get(K key);

	/**
	 * Load the value from the cache and compute it if it can't be found.
	 * 
	 * @param key
	 * @param mappingFunction
	 * @return Found or computed value
	 */
	V get(K key, @Nonnull Function<? super K, ? extends V> mappingFunction);

	/**
	 * Disable the cache.
	 */
	void disable();

	/**
	 * Enable the cache.
	 */
	void enable();

	/**
	 * Return the current size of the cache.
	 * 
	 * @return
	 */
	long size();

}
