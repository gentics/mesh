package com.gentics.cailun.demo.verticle;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Language;

@NodeEntity
public class Page extends Content {

	private static final long serialVersionUID = -4927498999985839348L;

	public static final String TEASER_KEY = "teaser";
	public static final String TITLE_KEY = "title";

	public Page() {
	}

	public String getTeaser(Language language) {
		return getI18NProperty(language, TEASER_KEY);
	}

	public String getTitle(Language language) {
		return getI18NProperty(language, TITLE_KEY);
	}

}
