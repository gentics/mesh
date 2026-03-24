package com.gentics.mesh.parameter;

/**
 * Language parameters
 */
public interface LanguageParameters extends ParameterProvider {

	/**
	 * Query parameter key: {@value #LANGUAGES_QUERY_PARAM_KEY}
	 */
	public static final String LANGUAGES_QUERY_PARAM_KEY = "lang";


	/**
	 * Set the <code>{@value #LANGUAGES_QUERY_PARAM_KEY}</code> request parameter values.
	 * 
	 * @param languageTags
	 * @return Fluent API
	 */
	default LanguageParameters setLanguages(String... languageTags) {
		setParameter(LANGUAGES_QUERY_PARAM_KEY, convertToStr(languageTags));
		return this;
	}

	/**
	 * Return the <code>{@value #LANGUAGES_QUERY_PARAM_KEY}</code> request parameter values.
	 * 
	 * @return
	 */
	default String[] getLanguages() {
		String value = getParameter(LANGUAGES_QUERY_PARAM_KEY);
		String[] languages = null;
		if (value != null) {
			languages = value.split(",");
		}
		return languages;
	}
}
