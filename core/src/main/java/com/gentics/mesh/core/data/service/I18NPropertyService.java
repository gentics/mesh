package com.gentics.mesh.core.data.service;

import com.gentics.mesh.core.data.model.tinkerpop.I18NProperties;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.data.model.tinkerpop.Translated;

public interface I18NPropertyService {

	I18NProperties create(Language language);

	Translated create(Tag tag, I18NProperties properties, Language language);

}
