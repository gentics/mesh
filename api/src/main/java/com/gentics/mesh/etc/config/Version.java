package com.gentics.mesh.etc.config;

import org.apache.commons.lang3.StringUtils;

/**
 * OpenAPI specification version
 */
public enum Version {

	/**
	 * v3.0
	 */
	V30(0),
	/**
	 * v3.1
	 */
	V31(1);

	private final int level;

	private Version(int level) {
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

	/**
	 * Pretty print the version
	 * 
	 * @return
	 */
	public String pretty() {
		switch(this) {
		case V30:
			return "3.0";
		case V31:
			return "3.1";
		}
		return null;
	}
}
