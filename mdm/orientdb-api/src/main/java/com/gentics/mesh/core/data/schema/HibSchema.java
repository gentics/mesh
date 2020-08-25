package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.rest.schema.SchemaReference;

public interface HibSchema extends HibFieldSchemaElement {

	HibSchemaVersion getLatestVersion();

	SchemaReference transformToReference();

	void deleteElement();

}
