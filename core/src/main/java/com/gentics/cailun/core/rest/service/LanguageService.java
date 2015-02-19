package com.gentics.cailun.core.rest.service;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.service.generic.GenericNodeService;

public interface LanguageService extends GenericNodeService<Language> {

	Language findByName(String string);

}
