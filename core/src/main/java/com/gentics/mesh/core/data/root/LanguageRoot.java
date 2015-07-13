package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

public interface LanguageRoot extends RootVertex<Language, LanguageResponse> {

	Language create(String languageName, String languageTag);

	void addLanguage(Language language);

	Language findByLanguageTag(String languageTag);

	Language getTagDefaultLanguage();

}
