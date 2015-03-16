package com.gentics.cailun.core.data.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.generic.GenericContent;

@NodeEntity
public class Content extends GenericContent {

	private static final long serialVersionUID = -4927498999985839348L;

	public static final String TEASER_KEY = "teaser";
	public static final String TITLE_KEY = "title";

	private String schema = null;

	public Content() {
		this.schema = "content";
	}

	public String getTeaser(Language language) {
		return getProperty(language, TEASER_KEY);
	}

	public String getTitle(Language language) {
		return getProperty(language, TITLE_KEY);
	}

	public String getSchema() {
		// TODO use labels instead
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

}
