package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.root.impl.LanguageRootImpl;

public interface LanguageRoot extends RootVertex<Language> {

	Language create(String languageName, String languageTag);

	void addLanguage(Language language);

	LanguageRootImpl getImpl();

	Language findByLanguageTag(String languageTag);

	Language getTagDefaultLanguage();

}
