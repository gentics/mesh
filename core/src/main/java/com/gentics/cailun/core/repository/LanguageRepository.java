package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

public interface LanguageRepository extends GenericNodeRepository<Language> {

	public Language findByName(String name);

	/**
	 * Find the language with the specified http://en.wikipedia.org/wiki/IETF_language_tag[IETF language tag]. 
	 * @param languageTag
	 * @return Found language or null if none could be found
	 */
	public Language findByLanguageTag(String languageTag);

}
