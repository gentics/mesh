package com.gentics.mesh.search;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.search.index.IndexHandler;

/**
 * Central location to register search index handlers.
 */
@Component
public class IndexHandlerRegistry {

	private Map<String, IndexHandler> handlers = new HashMap<>();

	/**
	 * Register the given handler.
	 * 
	 * @param handler
	 */
	public void registerHandler(IndexHandler handler) {
		handlers.put(handler.getClass().getName(), handler);
	}

	/**
	 * Unregister the given handler.
	 * 
	 * @param handler
	 */
	public void unregisterHandler(IndexHandler handler) {
		handlers.remove(handler.getClass().getName());
	}

	/**
	 * Return a collection which contains all registered handlers.
	 * 
	 * @return
	 */
	public Collection<IndexHandler> getHandlers() {
		return handlers.values();
	}

}
