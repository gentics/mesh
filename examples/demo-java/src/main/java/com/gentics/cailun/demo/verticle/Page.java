package com.gentics.cailun.demo.verticle;

import lombok.NoArgsConstructor;

import com.gentics.cailun.core.rest.model.GenericContent;

@NoArgsConstructor
public class Page extends GenericContent {

	private static final long serialVersionUID = 8079084130242382986L;
	private static final String CONTENT_KEY = "content";
	private static final String TEASER_KEY = "teaser";
	private static final String TITLE_KEY = "title";

	public Page(String name) {
		this.setName(name);
	}

	public void setContent(String html) {
		addProperty(CONTENT_KEY, html);
	}

	public String getContent() {
		return getProperty(CONTENT_KEY);
	}

	public void setTeaser(String text) {
		addProperty(TEASER_KEY, text);
	}

	public String getTeaser() {
		return getProperty(TEASER_KEY);
	}

	public String getTitle() {
		return getProperty(TITLE_KEY);
	}

	public void setTitle(String text) {
		addProperty(TITLE_KEY, text);
	}

}
