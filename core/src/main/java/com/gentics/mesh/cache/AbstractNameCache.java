package com.gentics.mesh.cache;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.data.NamedBaseElement;
import com.gentics.mesh.core.data.NamedElement;
import com.gentics.mesh.core.rest.MeshEvent;

/**
 * General implementation for the named elements cache.
 * 
 * @author plyhun
 *
 * @param <V>
 */
public abstract class AbstractNameCache<V extends NamedBaseElement> extends AbstractMeshCache<String, V> implements NameCache<V> {

	public static final long CACHE_SIZE = 100;

	/**
	 * Ctor
	 * 
	 * @param name cache name, must be unique
	 * @param factory
	 * @param registry
	 * @param events events for the cache to invalidate upon
	 */
	protected AbstractNameCache(String name, EventAwareCacheFactory factory, CacheRegistry registry, MeshEvent[] events) {
		this(name, factory, registry, CACHE_SIZE, events);
	}

	/**
	 * Ctor
	 * 
	 * @param name cache name, must be unique
	 * @param factory 
	 * @param registry
	 * @param maxSize max cache size
	 * @param events events for the cache to invalidate upon
	 */
	protected AbstractNameCache(String name, EventAwareCacheFactory factory, CacheRegistry registry, long maxSize, MeshEvent[] events) {
		super(createCache(name, events, factory, maxSize), registry, maxSize);
	}

	protected static <V extends NamedElement> EventAwareCache<String, V> createCache(String name, MeshEvent[] events, EventAwareCacheFactory factory, long maxSize) {
		return factory.<String, V>builder()
			.events(events)
			.action((event, cache) -> {
				cache.invalidate();
			})
			.name(name)
			.maxSize(maxSize)
			.build();
	}
}
