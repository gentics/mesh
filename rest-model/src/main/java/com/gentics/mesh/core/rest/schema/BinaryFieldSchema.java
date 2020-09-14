package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;

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

	@Override
	default boolean isDisplayField() {
		return true;
	}

	/**
	 * The parsing behaviour for this field.
	 * Setting this to something other than null or {@link BinaryFieldParserOption#DEFAULT} will override the global configuration.
	 *
	 * @see BinaryFieldParserOption
	 * @see MeshUploadOptions#isParser()
	 * @see ElasticSearchOptions#isIncludeBinaryFields()
	 *
	 * @return
	 */
	BinaryFieldParserOption getParserOption();
}
