package com.gentics.cailun.core.data.service.generic;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.generic.GenericContent;

@Component
@Transactional
public class GenericContentServiceImpl<T extends GenericContent> extends GenericFileServiceImpl<T> implements GenericContentService<T> {

	public void createLink(T from, T to) {
		// TODO maybe extract information about link start and end to speedup rendering of page with links
		// Linked link = new Linked(this, page);
		// this.links.add(link);
	}

	public void addI18Content(T content, Language language, String text) {
		setProperty(content, language, GenericContent.CONTENT_KEYWORD, text);
	}

	public void setContent(T content, Language language, String text) {
		setProperty(content, language, GenericContent.CONTENT_KEYWORD, text);
	}

}
