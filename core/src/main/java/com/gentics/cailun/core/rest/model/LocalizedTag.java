package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class LocalizedTag extends LocalizedNode {

	public LocalizedTag(String name) {
		this.name = name;
	}

}
