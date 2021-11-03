package com.gentics.mesh.core.data.schema;

import java.util.Objects;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

/**
 * A schema container is a graph element which stores the JSON schema data.
 */
public interface Schema extends GraphFieldSchemaContainer<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchema, HibSchemaVersion>, HibSchema,  GraphDBBucketableElement {

	/**
	 * Compose the index name for the schema index.
	 * 
	 * @return
	 */
	static String composeIndexName() {
		return "schemacontainer";
	}

	/**
	 * Compose the documentId for schema index documents.
	 * 
	 * @param schemaContainerUuid
	 * @return
	 */
	static String composeDocumentId(String schemaContainerUuid) {
		Objects.requireNonNull(schemaContainerUuid, "A schemaContainerUuid must be provided.");
		return schemaContainerUuid;
	}

	@Override
	default SchemaReference transformToReference() {
		return HibSchema.super.transformToReference();
	}

	@Override
	default TypeInfo getTypeInfo() {
		return HibSchema.super.getTypeInfo();
	}
}
