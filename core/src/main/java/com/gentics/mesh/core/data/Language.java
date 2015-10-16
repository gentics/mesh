package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.lang.LanguageResponse;

public interface Language extends GenericVertex<LanguageResponse>, NamedVertex {

	public static final String TYPE = "language";

	/**
	 * Return the native name of the language.
	 * 
	 * @return
	 */
	String getNativeName();

	/**
	 * Set the native name of the language.
	 * 
	 * @param languageNativeName
	 */
	void setNativeName(String languageNativeName);

	/**
	 * Return the language tag.
	 * 
	 * @return
	 */
	String getLanguageTag();

	/**
	 * Set the language tag.
	 * 
	 * @param languageTag
	 */
	void setLanguageTag(String languageTag);

}
