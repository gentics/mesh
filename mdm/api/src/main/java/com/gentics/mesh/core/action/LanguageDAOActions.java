package com.gentics.mesh.core.action;

import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

public interface LanguageDAOActions extends DAOActions<HibLanguage, LanguageResponse> {

	HibLanguage loadByTag(DAOActionContext ctx, String languageTag);
}
