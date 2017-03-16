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

	/**
	 * Set the <code>{@value #LANGUAGES_QUERY_PARAM_KEY}</code> request parameter values.
	 * 
	 * @param languageTags
	 * @return Fluent API
	 */
	public NodeParameters setLanguages(String... languageTags);

	/**
	 * Return the <code>{@value #LANGUAGES_QUERY_PARAM_KEY}</code> request parameter values.
	 * 
	 * @return
	 */
	String[] getLanguages();

	/**
	 * Set the <code>{@value #RESOLVE_LINKS_QUERY_PARAM_KEY}</code> request parameter.
	 * 
	 * @param type
	 * @return Fluent API
	 */
	NodeParameters setResolveLinks(LinkType type);

	/**
	 * Set the <code>{@value #EXPANDALL_QUERY_PARAM_KEY}</code> request parameter flag.
	 * 
	 * @param flag
	 * @return
	 * @deprecated This feature will be removed in a future mesh version due to graphql support
	 */
	@Deprecated
	NodeParameters setExpandAll(boolean flag);

	/**
	 * Return the <code>{@value #EXPANDALL_QUERY_PARAM_KEY}</code> query parameter flag value.
	 * 
	 * @return
	 * @deprecated This feature will be removed in a future mesh version due to graphql support
	 */
	@Deprecated
	boolean getExpandAll();

	/**
	 * Set a list of field names which should be expanded.
	 * 
	 * @param fieldNames
	 * @deprecated This feature will be removed in a future mesh version due to graphql support
	 */
	@Deprecated
	NodeParameters setExpandedFieldNames(String... fieldNames);

	/**
	 * Return the field names which should be expanded.
	 * 
	 * @return
	 * @deprecated This feature will be removed in a future mesh version due to graphql support
	 */
	@Deprecated
	String[] getExpandedFieldNames();

}
