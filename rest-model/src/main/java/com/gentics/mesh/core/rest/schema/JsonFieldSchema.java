package com.gentics.mesh.core.rest.schema;

import io.vertx.reactivex.json.schema.JsonSchema;

/**
 * REST POJO for a JSON object field schema.
 */
public interface JsonFieldSchema extends FieldSchema {

	/**
	 * Return a list of values which are allowed for this field. Null if no value restriction set
	 * 
	 * @return Allowed values
	 */
	JsonSchema[] getAllowedSchemas();

	/**
	 * Set the list of values which are allowed for this field. Set to null to remove value restriction
	 * 
	 * @param allowedSchemas
	 *            Allowed values or null
	 * @return Fluent API
	 */
	JsonFieldSchema setAllowedSchemas(JsonSchema... allowedSchemas);
}
