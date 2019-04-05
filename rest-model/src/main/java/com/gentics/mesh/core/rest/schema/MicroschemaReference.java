package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.common.NameUuidReference;

public interface MicroschemaReference extends NameUuidReference<MicroschemaReference> {

	/**
	 * Return the microschema version.
	 * 
	 * @return
	 */
	String getVersion();

	/**
	 * Set the microschema version.
	 * 
	 * @param version
	 * @return fluent API
	 */
	MicroschemaReference setVersion(String version);

	/**
	 * Return the uuid of the microschema version
	 * @return
	 */
	String getVersionUuid();

	/**
	 * Set the uuid of the microschema version
	 * @return
	 */
	MicroschemaReference setVersionUuid(String uuid);
}
