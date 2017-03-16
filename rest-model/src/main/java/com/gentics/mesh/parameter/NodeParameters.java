package com.gentics.mesh.parameter;

public interface NodeParameters extends ParameterProvider {

	/**
	 * Query parameter key: {@value #LANGUAGES_QUERY_PARAM_KEY}
	 */
	public static final String LANGUAGES_QUERY_PARAM_KEY = "lang";

	/**
	 * Query parameter key: {@value #EXPANDFIELDS_QUERY_PARAM_KEY}
	 */
	public static final String EXPANDFIELDS_QUERY_PARAM_KEY = "expand";

	/**
	 * Query parameter key: {@value #EXPANDALL_QUERY_PARAM_KEY}
	 */
	public static final String EXPANDALL_QUERY_PARAM_KEY = "expandAll";

	/**
	 * Query parameter key: {@value #RESOLVE_LINKS_QUERY_PARAM_KEY}
	 */
	public static final String RESOLVE_LINKS_QUERY_PARAM_KEY = "resolveLinks";

}
