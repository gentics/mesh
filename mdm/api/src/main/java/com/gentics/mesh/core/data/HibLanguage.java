package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.lang.LanguageResponse;

/**
 * Domain model for languages.
 */
public interface HibLanguage extends HibNamedElement<LanguageResponse> {

	/**
	 * Native name of the language.
	 * 
	 * @return
	 */
	String getNativeName();

	/**
	 * ISO 639-1 code of the language.
	 * 
	 * @return
	 */
	String getLanguageTag();

	/**
	 * Set the native name of the language.
	 * 
	 * @param languageNativeName
	 */
	void setNativeName(String languageNativeName);

	/**
	 * Set the language tag.
	 * 
	 * @param languageTag
	 */
	void setLanguageTag(String languageTag);
}
