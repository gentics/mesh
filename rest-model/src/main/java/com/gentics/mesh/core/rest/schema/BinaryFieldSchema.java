package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;

/**
 * REST POJO for a binary field schema.
 */
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
	 * The behaviour for extracting content or metadata from the binary data of this field.
	 * Setting this to something other than null will override the global configuration.
	 *
	 * @see MeshUploadOptions#isParser()
	 * @see ElasticSearchOptions#isIncludeBinaryFields()
	 *
	 * @return
	 */
	BinaryExtractOptions getBinaryExtractOptions();

	/**
	 * Set the binary extract options.
	 *
	 * @param extract
	 * @return
	 */
	BinaryFieldSchema setBinaryExtractOptions(BinaryExtractOptions extract);

	/**
	 * Returns the check service URL.
	 *
	 * @return The check service URL.
	 */
	String getCheckServiceUrl();

	/**
	 * Set the check service URL.
	 * @param checkServiceUrl The check service URL.
	 * @return Fluent API.
	 */
	BinaryFieldSchema setCheckServiceUrl(String checkServiceUrl);
}
