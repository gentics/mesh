package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.rest.model.generic.GenericNode;

@NodeEntity
public class Language extends GenericNode {

	private static final long serialVersionUID = 8621659419142532208L;

	@Indexed
	protected String name;

	/**
	 * RFC5646 specific language tag
	 */
	private String languageTag;

	@SuppressWarnings("unused")
	private Language() {

	}

	public Language(String name) {
		this.name = name;
	}

	public void setLanguageTag(String languageTag) {
		this.languageTag = languageTag;
	}

	public String getLanguageTag() {
		return languageTag;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
