package com.gentics.mesh.search.index;

import com.gentics.mesh.core.data.MeshCoreVertex;

import io.vertx.core.json.JsonObject;

/**
 * Transformator which can be used to transform a mesh specific element type into an object which is specific to the search provider implementation.
 * 
 * @param <T>
 */
public interface Transformer<T> {

	JsonObject toDocument(T object);

	/**
	 * Return the index type specific the mapping properties as JSON.
	 * 
	 * @return
	 */
	JsonObject getMappingProperties();

	/**
	 * Return the type specific elastic search mapping for the given type.
	 * 
	 * @param type
	 * @return
	 */
	JsonObject getMapping(String type);

	/**
	 * Create the json document for a permission update.
	 * 
	 * @param element
	 * @return
	 */
	JsonObject toPermissionPartial(MeshCoreVertex<?, ?> element);

}
