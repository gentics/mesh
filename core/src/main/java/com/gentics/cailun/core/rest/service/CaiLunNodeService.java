package com.gentics.cailun.core.rest.service;

import com.gentics.cailun.core.rest.model.CaiLunNode;
import com.gentics.cailun.core.rest.model.Language;

public interface CaiLunNodeService {

	public void setI18NProperty(CaiLunNode node, Language language, String key, String value);

	public void setName(CaiLunNode node, Language language, String name);

}
