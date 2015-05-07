package com.gentics.mesh.core.data.service;

import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.service.generic.GenericNodeService;

public interface LanguageService extends GenericNodeService<Language> {

	Language findByName(String string);

	Language findByLanguageTag(String languageTag);

}
