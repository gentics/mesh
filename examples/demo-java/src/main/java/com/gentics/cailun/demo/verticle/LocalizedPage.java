package com.gentics.cailun.demo.verticle;

import lombok.NoArgsConstructor;

import com.gentics.cailun.core.rest.model.LocalizedContent;

@NoArgsConstructor
public class LocalizedPage extends LocalizedContent {

	private static final long serialVersionUID = 8079084130242382986L;
	private static final String TEASER_KEY = "teaser";
	private static final String TITLE_KEY = "title";

	public LocalizedPage(String name) {
		this.setName(name);
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
