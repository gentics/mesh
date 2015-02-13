package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class Content extends LocalizableCaiLunNode<LocalizedContent> {

	private static final long serialVersionUID = 7918024043584207109L;

	public LocalizedContent addLocalizedContent(Language language, String name) {
		LocalizedContent content = new LocalizedContent(name);
		content.setLanguage(language);
		this.addLocalisation(content);
		return content;
	}

}
