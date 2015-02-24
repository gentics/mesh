package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

public interface LanguageRepository extends GenericNodeRepository<Language> {

	public Language findByName(String name);

	public Language findByLanguageTag(String languageTag);

}
