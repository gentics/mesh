package com.gentics.mesh.search;

import com.gentics.mesh.cache.MeshCache;

import io.vertx.core.json.JsonObject;

/**
 * Interface for the cache of expected search mappings
 */
public interface SearchMappingsCache extends MeshCache<String, JsonObject> {
	/**
	 * Put the value into the cache
	 * @param key cache key
	 * @param value cached value
	 */
	void put(String key, JsonObject value);
}
