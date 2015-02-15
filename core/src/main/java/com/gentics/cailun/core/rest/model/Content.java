package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class Content<T extends LocalizedContent> extends LocalizableCaiLunNode<T> {

	private static final long serialVersionUID = 7918024043584207109L;

	private Content() {
	}

	public Content(Language language, String name, String filename) {
		// getLocalisations().add(new LocalizedContent(language, name, filename));
		addLocalizedContent(language, name, filename);
	}

	public T addLocalizedContent(Language language, String name, String filename) {
		LocalizedContent content = new LocalizedContent(language, name, filename);
		content.setLanguage(language);
		this.addLocalization((T) content);
		return (T) content;
	}

}
