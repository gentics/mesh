package com.gentics.mesh.search.index;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.handler.DataHolderContext;

import io.vertx.core.json.JsonObject;

/**
 * Transformer which can be used to transform a mesh specific element type into an object which is specific to the search provider implementation.
 * 
 * @param <T>
 */
public interface Transformer<T> {

	/**
	 * Transform the given object into a JsonObject which can be used for storage.
	 * 
	 * @param object
	 * @param dhc data holder context
	 * @return
	 */
	JsonObject toDocument(T object, DataHolderContext dhc);

	/**
	 * Create the JSON document for a permission update.
	 * 
	 * @param element
	 * @return
	 */
	JsonObject toPermissionPartial(HibBaseElement element);

	/**
	 * Generate the version for the given element.
	 * 
	 * @param element
	 * @param dhc data holder context
	 * @return
	 */
	String generateVersion(T element, DataHolderContext dhc);

}
