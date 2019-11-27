package com.gentics.mesh.util;

/**
 * Various utility functions regarding Strings.
 */
public final class StringUtil {

	private StringUtil() {
	}

	/**
	 * Lower cases the first character of the string
	 * @param string
	 * @return
	 */
	public static String lowerCaseFirstChar(String string) {
		return string.substring(0, 1).toLowerCase() + string.substring(1);
	}
}
