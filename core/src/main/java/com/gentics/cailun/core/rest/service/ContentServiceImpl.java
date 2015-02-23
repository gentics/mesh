package com.gentics.cailun.core.rest.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.response.GenericContentResponse;
import com.gentics.cailun.core.rest.service.generic.GenericContentServiceImpl;

@Component
@Transactional
public class ContentServiceImpl extends GenericContentServiceImpl<Content> implements ContentService {

	@Autowired
	LanguageService languageService;

	public void setTeaser(Content page, Language language, String text) {
		setProperty(page, language, Content.TEASER_KEY, text);
	}

	public void setTitle(Content page, Language language, String text) {
		setProperty(page, language, Content.TITLE_KEY, text);
	}

	@Override
	public GenericContentResponse getReponseObject(Content content, List<String> languages) {
		if (languages.size() == 0) {
			// TODO return page with all languages?
			return null;
		}
		// Return page with flatted properties since only one language has been specified
		else if (languages.size() == 1) {

			String languageKey = languages.iterator().next();
			Language language = languageService.findByName(languageKey);
			GenericContentResponse response = new GenericContentResponse();
			response.setName(content.getName(language));
			response.setFilename(content.getFilename(language));
			response.setAuthor(content.getCreator());
			response.setContent(content.getContent(language));
			response.setTeaser(content.getTeaser(language));
			return response;
		} else {
			// TODO return all languages

			return null;
		}
	}

}
