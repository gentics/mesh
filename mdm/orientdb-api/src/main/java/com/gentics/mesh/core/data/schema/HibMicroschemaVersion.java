package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

public interface HibMicroschemaVersion extends HibFieldSchemaVersionElement<MicroschemaResponse, MicroschemaVersionModel, HibMicroschema, HibMicroschemaVersion> {

	MicroschemaReference transformToReference();

	// TODO MDM rename method
	HibMicroschema getSchemaContainer();

	// TODO MDM rename method
	void setSchemaContainer(HibMicroschema container);

	HibMicroschemaVersion getPreviousVersion();

	HibMicroschemaVersion getNextVersion();

	void setNextVersion(HibMicroschemaVersion version);

}
