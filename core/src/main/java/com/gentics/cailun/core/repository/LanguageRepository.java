package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.LanguageRoot;
import com.gentics.cailun.core.repository.action.LanguageActions;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

public interface LanguageRepository extends GenericNodeRepository<Language>, LanguageActions {

	public Language findByName(String name);

	/**
	 * Find the language with the specified http://en.wikipedia.org/wiki/IETF_language_tag[IETF language tag].
	 * 
	 * @param languageTag
	 * @return Found language or null if none could be found
	 */
	public Language findByLanguageTag(String languageTag);

	@Query("MATCH (n:LanguageRoot) return n")
	public LanguageRoot findRoot();

}
