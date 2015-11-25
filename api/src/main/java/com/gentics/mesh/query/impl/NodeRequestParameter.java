package com.gentics.mesh.query.impl;

import com.gentics.mesh.query.QueryParameterProvider;

public class NodeRequestParameter implements QueryParameterProvider {

	public static final String LANGUAGES_QUERY_PARAM_KEY = "lang";

	public static final String EXPANDFIELDS_QUERY_PARAM_KEY = "expand";

	public static final String EXPANDALL_QUERY_PARAM_KEY = "expandAll";

	public static final String RESOLVE_LINKS_QUERY_PARAM_KEY = "resolveLinks";

	private String[] languages;
	private String[] expandedFieldNames;
	private Boolean expandAll;
	private Boolean resolveLinks;

	/**
	 * Set the <code>lang</code> request parameter values.
	 * 
	 * @param languages
	 * @return
	 */
	public NodeRequestParameter setLanguages(String... languages) {
		this.languages = languages;
		return this;
	}

	/**
	 * Set a list of field names which should be expanded.
	 * 
	 * @param fieldNames
	 * @return
	 */
	public NodeRequestParameter setExpandedFieldNames(String... fieldNames) {
		this.expandedFieldNames = fieldNames;
		return this;
	}

	/**
	 * Set the <code>expandAll</code> request parameter flag.
	 * 
	 * @param flag
	 * @return
	 */
	public NodeRequestParameter setExpandAll(boolean flag) {
		this.expandAll = flag;
		return this;
	}

	/**
	 * Set the <code>resolveLinks</code> request parameter flag.
	 * 
	 * @param flag
	 */
	public NodeRequestParameter setResolveLinks(boolean flag) {
		this.resolveLinks = flag;
		return this;
	}

	@Override
	public String getQueryParameters() {
		StringBuilder query = new StringBuilder();
		if (languages != null && languages.length > 0) {
			query.append(LANGUAGES_QUERY_PARAM_KEY + "=");
			for (int i = 0; i < languages.length; i++) {
				query.append(languages[i]);
				if (i != languages.length - 1) {
					query.append(',');
				}
			}
		}

		if (expandedFieldNames != null && expandedFieldNames.length > 0) {
			if (query.length() != 0) {
				query.append("&");
			}
			query.append("expand=");
			for (int i = 0; i < expandedFieldNames.length; i++) {
				query.append(expandedFieldNames[i]);
				if (i != expandedFieldNames.length - 1) {
					query.append(',');
				}
			}
		}

		if (expandAll != null && expandAll == true) {
			if (query.length() != 0) {
				query.append("&");
			}
			query.append(EXPANDALL_QUERY_PARAM_KEY + "=true");
		}

		if (resolveLinks != null && resolveLinks == true) {
			if (query.length() != 0) {
				query.append("&");
			}
			query.append(RESOLVE_LINKS_QUERY_PARAM_KEY + "=true");
		}
		return query.toString();
	}

	@Override
	public String toString() {
		return getQueryParameters();
	}

}
