package com.gentics.mesh.graphql.model;

/**
 * Container for link information.
 */
public class LinkInfo {

	private String link;
	private String languageTag;

	public LinkInfo(String languageTag, String link) {
		this.languageTag = languageTag;
		this.link = link;
	}

	public String getLanguageTag() {
		return languageTag;
	}

	public String getLink() {
		return link;
	}

}
