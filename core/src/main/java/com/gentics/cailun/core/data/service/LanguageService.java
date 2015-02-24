package com.gentics.cailun.core.data.service;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;

public interface LanguageService extends GenericNodeService<Language> {

	Language findByName(String string);

	Language findByLanguageTag(String languageTag);

}
