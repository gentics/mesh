package com.gentics.mesh.hibernate.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Utilities class for calculating a "weight" for Strings
 */
public class StringScale {

	/**
	 * Get the "weight" of a string
	 * @param string string to weight
	 * @return weight of the string
	 */
	public static int getWeight(String string) {
		if (StringUtils.isEmpty(string)) {
			return 0;
		} else {
			// for now, we consider all strings to be encoded UTF-18 (with 2 bytes per character)
			return string.length() * 2;
		}
	}
}
