package com.gentics.cailun.core.data.service.generic;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.generic.GenericPropertyContainer;

public interface GenericPropertyContainerService<T extends GenericPropertyContainer> extends GenericNodeService<T> {

	public void setProperty(T node, Language language, String key, String value);

	public void setName(T node, Language language, String name);

}
