package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;

/**
 * REST POJO for a s3binary field schema.
 */
public interface S3BinaryFieldSchema extends FieldSchema {

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
	S3BinaryFieldSchema setAllowedMimeTypes(String... allowedMimeTypes);

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
	S3BinaryExtractOptions getS3BinaryExtractOptions();

	/**
	 * Set the binary extract options.
	 * 
	 * @param extract
	 * @return
	 */
	S3BinaryFieldSchema setS3BinaryExtractOptions(S3BinaryExtractOptions extract);
}
