package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;

import com.gentics.mesh.context.InternalActionContext;
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

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return CURRENT_API_BASE_PATH + "/schemas/" + getUuid();
	}
}
