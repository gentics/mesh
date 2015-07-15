package com.gentics.mesh.core.rest.node;

public class NodeRequestParameters implements QueryParameterProvider {

	String[] languages;

	public NodeRequestParameters setLanguages(String... languages) {
		this.languages = languages;
		return this;
	}

	@Override
	public String getQueryParameters() {
		StringBuilder query = new StringBuilder();
		if (languages != null && languages.length > 0) {
			query.append("lang=");
			for (int i = 0; i < languages.length; i++) {
				query.append(languages[i]);
				if (i != languages.length - 1) {
					query.append(',');
				}
			}
		}
		return query.toString();
	}
}
