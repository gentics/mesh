package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Language;

public interface LanguageRoot extends RootVertex<Language> {

	Language create(String languageName, String languageTag);

	void addLanguage(Language language);

	Language findByLanguageTag(String languageTag);

	Language getTagDefaultLanguage();

}
