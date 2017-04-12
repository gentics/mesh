package com.gentics.mesh.core.rest.schema;

public interface FieldSchemaContainerVersion extends FieldSchemaContainer {

	/**
	 * Return the container version.
	 * 
	 * @return
	 */
	int getVersion();

	/**
	 * Set the container version.
	 * 
	 * @param version
	 * @return Fluent API
	 */
	FieldSchemaContainerVersion setVersion(int version);

}
