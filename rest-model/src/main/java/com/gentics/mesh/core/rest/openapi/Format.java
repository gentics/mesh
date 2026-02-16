package com.gentics.mesh.core.rest.openapi;

import org.apache.commons.lang.StringUtils;

/**
 * OpenAPI output format
 */
public enum Format {
	/**
	 * Yaml
	 */
	YAML,
	/**
	 * JSON
	 */
	JSON;

	/**
	 * Safe parse string value
	 * 
	 * @param format
	 * @param defaultValue
	 * @return
	 */
	public static Format parse(String format, Format defaultValue) {
		if (StringUtils.isNotBlank(format)) {
			switch (format.toUpperCase()) {
			case "YAML": return YAML;
			case "JSON": return JSON;
			}
		}
		return defaultValue;
	}
}
