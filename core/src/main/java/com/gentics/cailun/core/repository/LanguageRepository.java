package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.repository.generic.GenericNodeRepository;
import com.gentics.cailun.core.rest.model.Language;

public interface LanguageRepository extends GenericNodeRepository<Language> {

	public Language findByName(String name);

}
