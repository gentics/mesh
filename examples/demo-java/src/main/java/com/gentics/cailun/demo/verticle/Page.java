package com.gentics.cailun.demo.verticle;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Language;

@NodeEntity
public class Page extends Content {

	private static final long serialVersionUID = -4927498999985839348L;

	private static final String TEASER_KEY = "teaser";
	private static final String TITLE_KEY = "title";

	public Page(Language language, String name, String filename) {
		super(language, name, filename);
	}

	public void setTeaser(Language language, String text) {
		setI18NProperty(language, TEASER_KEY, text);
	}

	public String getTeaser(Language language) {
		return getI18NProperty(language, TEASER_KEY);
	}

	public String getTitle(Language language) {
		return getI18NProperty(language, TITLE_KEY);
	}

	public void setTitle(Language language, String text) {
		setI18NProperty(language, TITLE_KEY, text);
	}

}
