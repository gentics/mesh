package com.gentics.mesh.handler;

import java.util.Map;

/**
 * Context which can hold data
 */
public interface DataHolderContext {
	/**
	 * Return the data map that is bound to this context.
	 * 
	 * @return Data map
	 */
	Map<String, Object> data();

	/**
	 * Add the data object for the given key to the data map.
	 * 
	 * @param key
	 *            Data key
	 * @param obj
	 *            Data object
	 * @return Fluent API
	 */
	DataHolderContext put(String key, Object obj);

	/**
	 * Return the data object for the given key.
	 * 
	 * @param key
	 *            Data key
	 * @return Data value or null when no value could be found for the given key
	 */
	<T> T get(String key);
}
