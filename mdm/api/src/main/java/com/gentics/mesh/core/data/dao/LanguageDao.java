package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

/**
 * DAO for language.
 */
public interface LanguageDao extends DaoGlobal<HibLanguage>, DaoTransformable<HibLanguage, LanguageResponse> {

	/**
	 * Return the language for the given language tag.
	 * 
	 * @param tag
	 * @return
	 */
	HibLanguage findByLanguageTag(String tag);

	/**
	 * Create the language.
	 * 
	 * @param languageName
	 * @param languageTag
	 * @return
	 */
	HibLanguage create(String languageName, String languageTag);

	@Override
	default LanguageResponse transformToRestSync(HibLanguage element, InternalActionContext ac, int level,
			String... languageTags) {
		LanguageResponse model = new LanguageResponse();
		model.setUuid(element.getUuid());
		model.setLanguageTag(element.getLanguageTag());
		model.setName(element.getName());
		model.setNativeName(element.getNativeName());
		return model;
	}

	@Override
	default String getAPIPath(HibLanguage element, InternalActionContext ac) {
		return element.getAPIPath(ac);
	}
}
