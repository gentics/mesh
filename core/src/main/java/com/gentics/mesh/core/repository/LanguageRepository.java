package com.gentics.mesh.core.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.RepositoryDefinition;

import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.LanguageRoot;
import com.gentics.mesh.core.repository.action.LanguageActions;
import com.gentics.mesh.core.repository.action.UUIDCRUDActions;

@RepositoryDefinition(domainClass = Language.class, idClass = Long.class)
public interface LanguageRepository extends UUIDCRUDActions<Language>, LanguageActions {

	public Language findByName(String name);

	public Language findOne(Long id);

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
