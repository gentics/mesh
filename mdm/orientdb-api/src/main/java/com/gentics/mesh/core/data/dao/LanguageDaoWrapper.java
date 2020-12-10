package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibLanguage;

public interface LanguageDaoWrapper extends LanguageDao, DaoGlobal<HibLanguage> {

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

	/**
	 * Find the language by name.
	 * 
	 * @param name
	 * @return
	 */
	HibLanguage findByName(String name);

}
