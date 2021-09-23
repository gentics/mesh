package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibLanguage;

/**
 * DAO for language.
 */
public interface LanguageDao extends DaoGlobal<HibLanguage> {

	/**
	 * Return the language for the given language tag.
	 * 
	 * @param tag
	 * @return
	 */
	HibLanguage findByLanguageTag(String tag);

	/**
	 * Create the language.
	 * 
	 * @param languageName
	 * @param languageTag
	 * @return
	 */
	HibLanguage create(String languageName, String languageTag);
}
