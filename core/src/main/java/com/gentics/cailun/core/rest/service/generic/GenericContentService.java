package com.gentics.cailun.core.rest.service.generic;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.generic.GenericContent;

public interface GenericContentService<T extends GenericContent> extends GenericFileService<T> {

	public void createLink(T from, T to);

	public void addI18Content(T content, Language language, String text);

	public void setContent(T content, Language language, String text);

}
