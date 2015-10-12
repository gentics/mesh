package com.gentics.mesh.core.rest.lang;

import com.gentics.mesh.core.rest.common.AbstractResponse;

public class LanguageResponse extends AbstractResponse {

	private String name;
	private String nativeName;
	private String languageTag;

	/**
	 * Return the language tag (eg. de)
	 * 
	 * @return
	 */
	public String getLanguageTag() {
		return languageTag;
	}

	/**
	 * Set the language tag.
	 * 
	 * @param languageTag
	 */
	public void setLanguageTag(String languageTag) {
		this.languageTag = languageTag;
	}

	/**
	 * Return the language name (eg. German)
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the language.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the language native name (eg. Deutsch)
	 * 
	 * @return
	 */
	public String getNativeName() {
		return nativeName;
	}

	/**
	 * Set the language native name.
	 * 
	 * @param nativeName
	 */
	public void setNativeName(String nativeName) {
		this.nativeName = nativeName;
	}
}
