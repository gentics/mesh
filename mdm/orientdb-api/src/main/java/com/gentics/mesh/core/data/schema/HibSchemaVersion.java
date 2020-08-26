package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;

public interface HibSchemaVersion extends HibFieldSchemaVersionElement<SchemaVersionModel> {

	HibSchemaVersion getPreviousVersion();

	HibSchemaVersion getNextVersion();

	// TODO rename method
	HibSchema getSchemaContainer();

	SchemaReference transformToReference();

	String getElementVersion();

	Iterable<? extends HibJob> referencedJobsViaTo();

}
