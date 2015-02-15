package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class LocalizedTag extends LocalizedNode {

	private static final long serialVersionUID = -3416638780569646056L;

	public LocalizedTag(Language language, String name) {
		super(language, name);
	}

}
