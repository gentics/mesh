package com.gentics.cailun.core.rest.service.generic;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.generic.GenericContent;

@Component
@Transactional
public class GenericContentServiceImpl<T extends GenericContent> extends GenericFileServiceImpl<T> implements GenericContentService<T> {

	public void createLink(T from, T to) {
		// TODO maybe extract information about link start and end to speedup rendering of page with links
		// Linked link = new Linked(this, page);
		// this.links.add(link);
	}

	public void addI18Content(T content, Language language, String text) {
		setI18NProperty(content, language, GenericContent.CONTENT_KEYWORD, text);
	}

	public void setContent(T content, Language language, String text) {
		setI18NProperty(content, language, GenericContent.CONTENT_KEYWORD, text);
	}

}
