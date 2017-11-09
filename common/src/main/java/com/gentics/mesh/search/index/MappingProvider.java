package com.gentics.mesh.search.index;

import io.vertx.core.json.JsonObject;

/**
 * A mapping provider is used to generate the needed search index mapping.
 */
public interface MappingProvider {

	/**
	 * Return the index type specific the mapping properties as JSON.
	 * 
	 * @return
	 */
	JsonObject getMappingProperties();

	/**
	 * Return the type specific elastic search mapping.
	 * 
	 * @return
	 */
	JsonObject getMapping();
}
