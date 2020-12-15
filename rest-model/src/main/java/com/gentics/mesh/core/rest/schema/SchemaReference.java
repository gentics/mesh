package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.common.NameUuidReference;

/**
 * Name/UUID/Version reference to a schema.
 */
public interface SchemaReference extends NameUuidReference<SchemaReference> {

	/**
	 * Return the version of the referenced schema.
	 * 
	 * @return
	 */
	String getVersion();

	/**
	 * Set the version of the referenced schema.
	 * 
	 * @param version
	 * @return Fluent API
	 */
	SchemaReference setVersion(String version);

	/**
	 * Get the uuid of the schema version
	 * @return
	 */
	String getVersionUuid();

	/**
	 * Set the uuid of the schema version
	 * @param uuid
	 * @return
	 */
	SchemaReference setVersionUuid(String uuid);
}
