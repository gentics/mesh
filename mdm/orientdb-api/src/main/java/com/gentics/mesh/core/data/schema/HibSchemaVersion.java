package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;

public interface HibSchemaVersion extends HibFieldSchemaVersionElement {

	SchemaVersionModel getSchema();

	void setSchema(SchemaVersionModel schema);

	HibSchemaVersion getPreviousVersion();

	// TODO rename method
	HibSchema getSchemaContainer();

	SchemaReference transformToReference();

	SchemaChange<?> getNextChange();

	String getElementVersion();

	HibSchemaVersion getNextVersion();

	void deleteElement();

	Iterable<? extends HibJob> referencedJobsViaTo();
}
