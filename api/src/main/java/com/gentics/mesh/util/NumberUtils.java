package com.gentics.mesh.util;

import org.apache.commons.lang.StringUtils;

public final class NumberUtils extends org.apache.commons.lang.math.NumberUtils {

	/**
	 * Parse the given string into a integer and return the default value when the input string is null or empty.
	 * 
	 * @param str
	 * @param defaultValue
	 * @return
	 */
	public static Integer toInteger(String str, Integer defaultValue) {
		if (StringUtils.isEmpty(str)) {
			return defaultValue;
		}
		try {
			return Integer.valueOf(str);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
