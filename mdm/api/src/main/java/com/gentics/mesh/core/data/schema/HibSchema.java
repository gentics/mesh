package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

/**
 * Domain model for schema.
 */
public interface HibSchema extends HibFieldSchemaElement<SchemaResponse, SchemaVersionModel, HibSchema, HibSchemaVersion>, HibBucketableElement {

	/**
	 * Transform the schema to a reference POJO.
	 * 
	 * @return
	 */
	SchemaReference transformToReference();

	/**
	 * Delete the schema.
	 */
	void deleteElement();

}
