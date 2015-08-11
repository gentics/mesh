package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Language;

/**
 * Aggregation vertex for languages.
 */
public interface LanguageRoot extends RootVertex<Language> {

	/**
	 * Create a new language.
	 * 
	 * @param languageName
	 * @param languageTag
	 * @return
	 */
	Language create(String languageName, String languageTag);

	/**
	 * Add the given language to the aggregation node.
	 * 
	 * @param language
	 */
	void addLanguage(Language language);

	/**
	 * Find the language with the given language tag.
	 * 
	 * @param languageTag
	 * @return
	 */
	Language findByLanguageTag(String languageTag);

	/**
	 * Return the default language.
	 * 
	 * @return
	 */
	Language getTagDefaultLanguage();

}
