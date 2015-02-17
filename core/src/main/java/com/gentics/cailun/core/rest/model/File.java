package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class File extends CaiLunNode {

	private static final long serialVersionUID = -8945772390192195270L;

	public static final String FILENAME_KEYWORD = "filename";

	public String getFilename(Language language) {
		return getI18NProperty(language, FILENAME_KEYWORD);
	}
	

}
