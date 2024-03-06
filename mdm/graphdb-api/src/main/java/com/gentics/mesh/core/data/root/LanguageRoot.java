package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Language;

/**
 * Aggregation vertex for languages.
 */
public interface LanguageRoot extends RootVertex<Language> {

	/**
	 * Find the language with the given language tag.
	 * 
	 * @param languageTag
	 * @return Found language or null when no language could be found that matches the given tag
	 */
	Language findByLanguageTag(String languageTag);

}
