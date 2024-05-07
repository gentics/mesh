package com.gentics.mesh.parameter;

import org.apache.commons.lang.StringUtils;

/**
 * Project load query parameters.
 */
public interface ProjectLoadParameters extends ParameterProvider {

	public static final String LANGS_QUERY_PARAM_KEY = "langs";

	/**
	 * Get the flag to load project languages.
	 * 
	 * @return
	 */
	default boolean getLangs() {
		String param = getParameter(LANGS_QUERY_PARAM_KEY);
		if (StringUtils.isNotBlank(param)) {
			return Boolean.parseBoolean(param);
		}
		return false;
	}

	/**
	 * Set the flag to load project languages. 
	 * 
	 * @param langs
	 */
	default ProjectLoadParameters setLangs(boolean langs) {
		setParameter(LANGS_QUERY_PARAM_KEY, Boolean.toString(langs));
		return this;
	}
}
