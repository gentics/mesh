package com.gentics.mesh.search.index;

import com.gentics.mesh.core.data.MeshCoreVertex;

import io.vertx.core.json.JsonObject;

/**
 * Transformator which can be used to transform a mesh specific element type into an object which is specific to the search provider implementation.
 * 
 * @param <T>
 */
public interface Transformer<T> {

	/**
	 * Transform the given object into a JsonObject which can be used for storage.
	 * 
	 * @param object
	 * @return
	 */
	JsonObject toDocument(T object);

	/**
	 * Create the JSON document for a permission update.
	 * 
	 * @param element
	 * @return
	 */
	JsonObject toPermissionPartial(MeshCoreVertex<?, ?> element);

}
