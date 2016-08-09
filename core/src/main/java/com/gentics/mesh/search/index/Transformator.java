package com.gentics.mesh.search.index;

import io.vertx.core.json.JsonObject;

public interface Transformator<T> {

	JsonObject toDocument(T object);

	/**
	 * Return the index type specific the mapping properties as JSON.
	 * 
	 * @return
	 */
	JsonObject getMappingProperties();

	/**
	 * Return the type specific mapping.
	 * 
	 * @param type
	 * @return
	 */
	JsonObject getMapping(String type);

}
