package com.gentics.mesh.query.impl;

import com.gentics.mesh.query.QueryParameterProvider;

public class NodeRequestParameter implements QueryParameterProvider {

	public static final String LANGUAGES_QUERY_PARAM_KEY = "lang";

	public static final String EXPANDFIELDS_QUERY_PARAM_KEY = "expand";
	
	private String[] languages;
	private String[] expandedFieldNames;

	public NodeRequestParameter setLanguages(String... languages) {
		this.languages = languages;
		return this;
	}

	public NodeRequestParameter setExpandedFieldNames(String... fieldNames) {
		this.expandedFieldNames = fieldNames;
		return this;
	}

	@Override
	public String getQueryParameters() {
		StringBuilder query = new StringBuilder();
		if (languages != null && languages.length > 0) {
			query.append(LANGUAGES_QUERY_PARAM_KEY+"=");
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
		return query.toString();
	}
}
