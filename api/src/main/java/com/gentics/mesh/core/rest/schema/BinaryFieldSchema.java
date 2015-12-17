package com.gentics.mesh.core.rest.schema;

public interface BinaryFieldSchema extends FieldSchema {

	/**
	 * Return list of allowed mime types. When empty all types will be accepted.
	 * 
	 * @return
	 */
	String[] getAllowedMimeTypes();

	/**
	 * Set the list of allowed mime types.
	 * 
	 * @param allowedMimeTypes
	 * @return Fluent API
	 */
	BinaryFieldSchema setAllowedMimeTypes(String... allowedMimeTypes);

}
