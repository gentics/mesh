package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibLanguage;

public interface LanguageDaoWrapper extends LanguageDao, DaoGlobal<HibLanguage> {

	HibLanguage findByLanguageTag(String tag);

	HibLanguage create(String languageName, String languageTag);

	HibLanguage findByName(String name);

}
