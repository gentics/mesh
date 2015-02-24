package com.gentics.cailun.core.data.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.service.generic.GenericContentServiceImpl;
import com.gentics.cailun.core.rest.response.GenericContentResponse;
import com.gentics.cailun.core.rest.response.RestUser;

@Component
@Transactional
public class ContentServiceImpl extends GenericContentServiceImpl<Content> implements ContentService {

	@Autowired
	LanguageService languageService;

	@Autowired
	UserService userService;
	
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
			response.setLanguageTag(language.getLanguageTag());
			response.addProperty("name", content.getName(language));
			response.addProperty("filename", content.getFilename(language));
			RestUser restUser = userService.getResponseObject(content.getCreator());
			response.setAuthor(restUser);
			response.addProperty("content", content.getContent(language));
			response.addProperty("teaser", content.getTeaser(language));
			return response;
		} else {
			// TODO return all languages

			return null;
		}
	}

}
