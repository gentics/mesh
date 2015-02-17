package com.gentics.cailun.core.rest.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Language;

@Component
@Transactional
public class ContentServiceImpl extends FileServiceImpl implements ContentService {

	public void createLink(Content from, Content to) {
		// TODO maybe extract information about link start and end to speedup rendering of page with links
		// Linked link = new Linked(this, page);
		// this.links.add(link);
	}

	public void addI18Content(Content content, Language language, String text) {
		setI18NProperty(content, language, Content.CONTENT_KEYWORD, text);
	}
	

}
