package com.gentics.mesh.core.data.service;

import com.gentics.mesh.core.data.model.root.LanguageRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Language;

public interface LanguageService {

	Language findByName(String string);

	Language findByLanguageTag(String languageTag);

	LanguageRoot createRoot();

	Language create(String languageName, String languageTag);

	LanguageRoot findRoot();

}
