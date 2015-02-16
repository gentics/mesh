package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class File extends CaiLunNode {

	private static final long serialVersionUID = -8945772390192195270L;

	public static final String FILENAME_KEYWORD = "filename";

	public void setFilename(Language language, String filename) {
		setI18NProperty(language, FILENAME_KEYWORD, filename);
	}
}
