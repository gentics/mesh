package com.gentics.cailun.demo.verticle;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.service.ContentServiceImpl;

public class PageServiceImpl extends ContentServiceImpl implements PageService {

	public void setTeaser(Page page, Language language, String text) {
		setI18NProperty(page, language, Page.TEASER_KEY, text);
	}

	public void setTitle(Page page , Language language, String text) {
		setI18NProperty(page, language, Page.TITLE_KEY, text);
	}

}
