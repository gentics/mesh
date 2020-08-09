package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.Language;

public interface LanguageDaoWrapper extends LanguageDao {

	Language findByLanguageTag(String tag);

	long computeCount();

	Language create(String languageName, String languageTag);

}
