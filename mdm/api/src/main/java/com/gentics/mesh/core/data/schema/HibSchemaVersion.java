package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

public interface HibSchemaVersion extends HibFieldSchemaVersionElement<SchemaResponse, SchemaVersionModel, HibSchema, HibSchemaVersion> {

	// TODO MDM rename method
	HibSchema getSchemaContainer();

	// TODO MDM rename method
	void setSchemaContainer(HibSchema container);

	SchemaReference transformToReference();

	String getElementVersion();

	Iterable<? extends HibJob> referencedJobsViaTo();

	boolean isAutoPurgeEnabled();

}
