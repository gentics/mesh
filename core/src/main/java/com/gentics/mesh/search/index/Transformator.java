package com.gentics.mesh.search.index;

import io.vertx.core.json.JsonObject;

public interface Transformator<T> {

	JsonObject toDocument(T object);

	/**
	 * Return the index specific the mapping as JSON.
	 * 
	 * @return
	 */
	JsonObject getMappingProperties();

}
