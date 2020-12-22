package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

public interface HibMicroschemaVersion
	extends HibFieldSchemaVersionElement<MicroschemaResponse, MicroschemaVersionModel, HibMicroschema, HibMicroschemaVersion> {

	MicroschemaReference transformToReference();

	// TODO MDM rename method
	HibMicroschema getSchemaContainer();

	// TODO MDM rename method
	void setSchemaContainer(HibMicroschema container);

	/**
	 * Return the previous schema version
	 * 
	 * @return previous version or null when no previous version exists
	 * 
	 */
	HibMicroschemaVersion getPreviousVersion();

	/**
	 * Return the next schema version.
	 * 
	 * @return next version or null when no next version exists
	 */
	HibMicroschemaVersion getNextVersion();

	/**
	 * Set the next microschema version.
	 * 
	 * @param version
	 */
	void setNextVersion(HibMicroschemaVersion version);

}
