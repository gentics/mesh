package com.gentics.mesh.search;

public class SearchHelper {

	private SearchProvider searchProvider;

	public SearchHelper(SearchProvider searchProvider) {
		this.searchProvider = searchProvider;
	}

	public void init() {
		searchProvider.createIndex("user", "user");
		searchProvider.createIndex("group", "group");
		searchProvider.createIndex("role", "role");

		//TODO handle other languages as well
		searchProvider.createIndex("node", "node-en");
		searchProvider.createIndex("node", "node-de");
		searchProvider.createIndex("project", "project");
		searchProvider.createIndex("tagfamily", "tagfamily");
		searchProvider.createIndex("tag", "tag");
		searchProvider.createIndex("schema", "schema");

	}
}
