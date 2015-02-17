package com.gentics.cailun.demo.verticle;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.service.ContentServiceImpl;

@Component
@Transactional
public class PageServiceImpl extends ContentServiceImpl implements PageService {

	public void setTeaser(Page page, Language language, String text) {
		setI18NProperty(page, language, Page.TEASER_KEY, text);
	}

	public void setTitle(Page page, Language language, String text) {
		setI18NProperty(page, language, Page.TITLE_KEY, text);
	}

}
