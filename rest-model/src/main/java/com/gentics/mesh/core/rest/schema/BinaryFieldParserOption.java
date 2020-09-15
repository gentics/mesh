package com.gentics.mesh.core.rest.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;

/**
 * Determines how meta information of binary data is extracted and stored.
 *
 * @see MeshUploadOptions#isParser()
 * @see ElasticSearchOptions#isIncludeBinaryFields()
 */
public enum BinaryFieldParserOption {
	/**
	 * Uses the globally configured options to parse this binary.
	 *
	 * @see MeshUploadOptions#isParser()
	 * @see ElasticSearchOptions#isIncludeBinaryFields()
	 */
	@JsonProperty("default")
	DEFAULT,

	/**
	 * Does not parse or store any data of the binary field.
	 *
	 * This is equivalent to this global configuration:
	 * <ul>
	 *     <li>upload.parser: false</li>
	 *     <li>search.includeBinaryFields: false</li>
	 * </ul>
	 */
	@JsonProperty("none")
	NONE,

	/**
	 * Parses the binary data and stores meta information in Mesh. The data will not be available in Elasticsearch.
	 *
	 * This is equivalent to this global configuration:
	 * <ul>
	 *     <li>upload.parser: true</li>
	 *     <li>search.includeBinaryFields: false</li>
	 * </ul>
	 */
	@JsonProperty("parseOnly")
	PARSE_ONLY,

	/**
	 * Parses the binary data and stores meta information in Mesh and Elasticsearch.
	 *
	 * This is equivalent to this global configuration:
	 * <ul>
	 *     <li>upload.parser: true</li>
	 *     <li>search.includeBinaryFields: true</li>
	 * </ul>
	 */
	@JsonProperty("parseAndSearch")
	PARSE_AND_SEARCH
}
