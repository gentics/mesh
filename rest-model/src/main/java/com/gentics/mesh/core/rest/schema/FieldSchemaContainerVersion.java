package com.gentics.mesh.core.rest.schema;

/**
 * Schema definition of a schema container version.
 */
public interface FieldSchemaContainerVersion extends FieldSchemaContainer {

	/**
	 * Return the container version.
	 * 
	 * @return
	 */
	String getVersion();

	/**
	 * Set the container version.
	 * 
	 * @param version
	 * @return Fluent API
	 */
	FieldSchemaContainerVersion setVersion(String version);

}
