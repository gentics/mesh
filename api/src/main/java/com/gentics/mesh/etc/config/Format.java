package com.gentics.mesh.etc.config;

import org.apache.commons.lang3.StringUtils;

/**
 * OpenAPI output format
 */
public enum Format {
	/**
	 * Yaml
	 */
	YAML(0),
	/**
	 * JSON
	 */
	JSON(1);

	private final int level;

	private Format(int level) {
		this.level = level;
	}

	/**
	 * Get the filtering int value.
	 * 
	 * @return
	 */
	public int getLevel() {
		return level;
	}

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
