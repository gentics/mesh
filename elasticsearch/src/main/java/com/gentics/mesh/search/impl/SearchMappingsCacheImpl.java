package com.gentics.mesh.search.impl;

import java.time.temporal.ChronoUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.AbstractMeshCache;
import com.gentics.mesh.cache.CacheRegistry;
import com.gentics.mesh.cache.EventAwareCache;
import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.SearchMappingsCache;

import io.vertx.core.json.JsonObject;

/**
 * Implementation of the {@link SearchMappingsCache}
 */
@Singleton
public class SearchMappingsCacheImpl extends AbstractMeshCache<String, JsonObject> implements SearchMappingsCache {
	private static final long CACHE_SIZE = 1000;

	/**
	 * Create the cache
	 * @param factory factory
	 * @param options mesh options
	 * @return cache instance
	 */
	private static EventAwareCache<String, JsonObject> createCache(EventAwareCacheFactory factory, MeshOptions options) {
		return factory.<String, JsonObject>builder()
				.maxSize(CACHE_SIZE)
				.events(MeshEvent.STARTUP)
				.expireAfterAccess(options.getSearchOptions().getIndexMappingCacheTimeout(), ChronoUnit.MILLIS)
				.name("searchmappings")
				.build();
	}

	/**
	 * Create the instance
	 * @param factory cache factory
	 * @param registry cache registry
	 * @param options mesh options
	 */
	@Inject
	public SearchMappingsCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry, MeshOptions options) {
		super(createCache(factory, options), registry, CACHE_SIZE);
	}

	@Override
	public void put(String key, JsonObject value) {
		cache.put(key, value);
	}
}
