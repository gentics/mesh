package com.gentics.mesh.plugin.forms.util;

import java.util.List;

import com.gentics.mesh.plugin.PluginContext;

/**
 * Utilities for handling of parameters
 */
public final class ParameterUtil {
	/**
	 * Get the languages contained in the request as query parameter "lang". If the
	 * query parameter is not found, an empty array will be returned
	 * 
	 * @param context plugin context
	 * @return string array of languages
	 */
	public static String[] getLanguages(PluginContext context) {
		List<String> languageList = context.queryParam("lang");
		String[] languages = null;
		if (languageList != null) {
			languages = (String[]) languageList.toArray(new String[languageList.size()]);
		} else {
			languages = new String[0];
		}

		return languages;
	}

	public static String getFirstLanguage(PluginContext context, String def) {
		String[] languages = getLanguages(context);
		if (languages.length > 0) {
			return languages[0];
		} else {
			return def;
		}
	}

	/**
	 * Transform the given value into an integer, if possible. If this is not possible, return the default value
	 * @param value value to transform
	 * @param defaultValue default value
	 * @return transformed or default value
	 */
	public static int asInt(Object value, int defaultValue) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		} else if (value != null) {
			try {
				return Integer.parseInt(value.toString());
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		} else {
			return defaultValue;
		}
	}

	/**
	 * Transform the given value into a boolean. Return the default value, if the value is null.
	 * @param value value
	 * @param defaultValue default value
	 * @return transformed or default value
	 */
	public static boolean asBoolean(Object value, boolean defaultValue) {
		if (value instanceof Boolean) {
			return ((Boolean)value).booleanValue();
		} else if (value != null) {
			return Boolean.parseBoolean(value.toString());
		} else {
			return defaultValue;
		}
	}
}
