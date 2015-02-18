package com.gentics.cailun.core.rest.service.generic;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.generic.GenericNode;

public interface GenericNodeService<T extends GenericNode> {

	public void setI18NProperty(T node, Language language, String key, String value);

	public void setName(T node, Language language, String name);

	public T save(T node);

	public void delete(T node);
	
	public T findOne(Long id);

}
