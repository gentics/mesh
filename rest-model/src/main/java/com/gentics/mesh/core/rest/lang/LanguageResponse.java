package com.gentics.mesh.core.rest.lang;

import com.gentics.mesh.core.rest.common.AbstractResponse;

/**
 * POJO for a language response.
 */
public class LanguageResponse extends AbstractResponse {

	private String name;
	private String nativeName;
	private String languageTag;

	/**
	 * Return the ISO 639-1 language tag (eg. de)
	 * 
	 * @return Language tag of the language
	 */
	public String getLanguageTag() {
		return languageTag;
	}

	/**
	 * Set the ISO 639-1 language tag.
	 * 
	 * @param languageTag
	 *            Language tag
	 */
	public void setLanguageTag(String languageTag) {
		this.languageTag = languageTag;
	}

	/**
	 * Return the ISO 639-1 language name (eg. German)
	 * 
	 * @return Name of the language
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the ISO 639-1 name of the language.
	 * 
	 * @param name
	 *            Name of the language
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the language native name (eg. Deutsch)
	 * 
	 * @return Native name of the language
	 */
	public String getNativeName() {
		return nativeName;
	}

	/**
	 * Set the language native name.
	 * 
	 * @param nativeName
	 *            Native name of the language
	 */
	public void setNativeName(String nativeName) {
		this.nativeName = nativeName;
	}
}
