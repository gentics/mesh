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
}
