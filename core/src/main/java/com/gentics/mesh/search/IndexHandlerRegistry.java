package com.gentics.mesh.search;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.search.index.IndexHandler;

/**
 * Central location to register search index handlers.
 */
public class IndexHandlerRegistry {

	private Map<String, IndexHandler> handlers = Collections.synchronizedMap(new HashMap<>());

	private static IndexHandlerRegistry instance;

	@Inject
	public IndexHandlerRegistry() {
		instance = this;
	}

	/**
	 * Get the instance
	 * 
	 * @return instance
	 */
	public static IndexHandlerRegistry getInstance() {
		return instance;
	}

	/**
	 * Register the given handler.
	 * 
	 * @param handler
	 */
	public void registerHandler(IndexHandler handler) {
		handlers.put(handler.getKey(), handler);
	}

	/**
	 * Unregister the given handler.
	 * 
	 * @param handler
	 */
	public void unregisterHandler(IndexHandler handler) {
		handlers.remove(handler.getKey());
	}

	/**
	 * Return a collection which contains all registered handlers.
	 * 
	 * @return
	 */
	public Collection<IndexHandler> getHandlers() {
		return handlers.values();
	}

	/**
	 * Get the index handler with given key
	 * 
	 * @param key
	 *            index handler key
	 * @return index handler or null if not registered
	 */
	public IndexHandler get(String key) {
		if (!handlers.containsKey(key)) {
			throw new NotImplementedException("Index type {" + key + "} is not implemented.");
		}
		return handlers.get(key);
	}
}
