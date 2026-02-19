package com.gentics.mesh.parameter;

import com.gentics.mesh.core.rest.openapi.Format;
import com.gentics.mesh.core.rest.openapi.Version;

/**
 * Parameters for an OpenAPI definition request
 */
public interface OpenAPIParameters extends ParameterProvider {

	/**
	 * Key of the "version" parameter
	 */
	public static final String VERSION_PARAMETER_KEY = "version";

	/**
	 * Key of the "format" parameter
	 */
	public static final String FORMAT_PARAMETER_KEY = "format";

	/**
	 * Set the OpenAPI standard version 
	 * 
	 * @param version The specification version
	 * @return Fluent API
	 */
	default OpenAPIParameters setVersion(Version version) {
		setParameter(VERSION_PARAMETER_KEY, String.valueOf(version));
		return this;
	}

	/**
	 * Check whether OpenAPI spec version
	 * 
	 * @return value
	 */
	default Version getVersion() {
		return Version.parse(getParameter(VERSION_PARAMETER_KEY), Version.V30);
	}

	/**
	 * Set the format
	 * 
	 * @param format the specification format
	 * @return Fluent API
	 */
	default OpenAPIParameters setFormat(Format format) {
		setParameter(FORMAT_PARAMETER_KEY, String.valueOf(format));
		return this;
	}

	/**
	 * Check the spec format
	 * 
	 * @return value
	 */
	default Format getFormat() {
		return Format.parse(getParameter(FORMAT_PARAMETER_KEY), Format.JSON);
	}
}
