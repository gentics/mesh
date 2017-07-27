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
	 * @return Created language
	 */
	default Language create(String languageName, String languageTag) {
		return create(languageName, languageTag, null);
	}

	/**
	 * Create a new language.
	 * 
	 * @param languageName
	 * @param languageTag
	 * @param uuid optional uuid
	 * @return Created language
	 */
	Language create(String languageName, String languageTag, String uuid);

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
	 * @return Found language or null when no language could be found that matches the given tag
	 */
	Language findByLanguageTag(String languageTag);

}
