package com.gentics.mesh.search;

public class SearchHelper {

	private SearchProvider searchProvider;

	public SearchHelper(SearchProvider searchProvider) {
		this.searchProvider = searchProvider;
	}

	public void init() {
		searchProvider.createIndex("user");
		searchProvider.createIndex("group");
		searchProvider.createIndex("role");

		searchProvider.createIndex("node");
		searchProvider.createIndex("project");
		searchProvider.createIndex("tagfamily");
		searchProvider.createIndex("tag");
		searchProvider.createIndex("schema");

	}
}
