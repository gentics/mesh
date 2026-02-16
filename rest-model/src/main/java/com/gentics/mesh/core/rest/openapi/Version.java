package com.gentics.mesh.core.rest.openapi;

import org.apache.commons.lang.StringUtils;

/**
 * OpenAPI specification version
 */
public enum Version {

	/**
	 * v3.0
	 */
	V30,
	/**
	 * v3.1
	 */
	V31;

	/**
	 * Safe parse string value
	 * 
	 * @param version
	 * @param defaultValue
	 * @return
	 */
	public static Version parse(String version, Version defaultValue) {
		if (StringUtils.isNotBlank(version)) {
			switch (version.toUpperCase()) {
			case "V30": 
			case "3.0": 
				return V30;
			case "V31": 
			case "3.1": 
				return V31;
			}
		}
		return defaultValue;
	}
}
