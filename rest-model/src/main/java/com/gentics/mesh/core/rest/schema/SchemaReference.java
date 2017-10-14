package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.common.NameUuidReference;

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

}
