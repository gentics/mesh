package com.gentics.mesh.core.action;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

public interface LanguageDAOActions extends DAOActions<Language, LanguageResponse> {

	Language loadByTag(DAOActionContext ctx, String languageTag);
}
