package com.gentics.cailun.core.data.service.generic;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.generic.GenericContent;

public interface GenericContentService<T extends GenericContent> extends GenericFileService<T> {

	public void createLink(T from, T to);

	public void addI18Content(T content, Language language, String text);

	public void setContent(T content, Language language, String text);

}
