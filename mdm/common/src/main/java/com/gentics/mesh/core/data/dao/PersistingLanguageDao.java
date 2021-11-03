package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

/**
 * A persisting extension to {@link LanguageDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingLanguageDao extends LanguageDao, PersistingDaoGlobal<HibLanguage> {

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
