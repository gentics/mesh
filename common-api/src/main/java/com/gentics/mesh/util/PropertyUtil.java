package com.gentics.mesh.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for property handling.
 */
public final class PropertyUtil {

	private final static Pattern PATTERN = Pattern.compile("\\$\\{(\\w+)\\}|\\$(\\w+)");

	private PropertyUtil() {
	}

	/**
	 * Resolves found properties in the given input string value.
	 * 
	 * @param value
	 * @return
	 */
	public static String resolve(String value) {
		if (null == value) {
			return null;
		}

		Matcher m = PATTERN.matcher(value);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String envVarName = null == m.group(1) ? m.group(2) : m.group(1);
			String envVarValue = System.getenv(envVarName);
			if (envVarValue == null) {
				envVarValue = System.getProperty(envVarName);
			}
			m.appendReplacement(sb,
				null == envVarValue ? "" : Matcher.quoteReplacement(envVarValue));
		}
		m.appendTail(sb);
		return sb.toString();
	}
}
