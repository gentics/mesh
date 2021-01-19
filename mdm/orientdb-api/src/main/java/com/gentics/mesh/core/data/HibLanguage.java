package com.gentics.mesh.core.data;

/**
 * Domain model for languages.
 */
public interface HibLanguage extends HibBaseElement {

	/**
	 * Name of the language.
	 * 
	 * @return
	 */
	String getName();

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
}
