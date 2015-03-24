package com.gentics.cailun.core.data.model;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.data.model.generic.GenericNode;

@NodeEntity
public class Language extends GenericNode {

	private static final long serialVersionUID = 8621659419142532208L;

	@Indexed
	protected String name;

	protected String nativeName;

	/**
	 * http://en.wikipedia.org/wiki/IETF_language_tag#Syntax_of_language_tags[IETF language tag]
	 */
	@Indexed(unique = true)
	private String languageTag;

	@SuppressWarnings("unused")
	private Language() {

	}

	public Language(String name, String languageTag) {
		this.name = name;
		this.languageTag = languageTag;
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

	public String getNativeName() {
		return nativeName;
	}

	public void setNativeName(String nativeName) {
		this.nativeName = nativeName;
	}
}
