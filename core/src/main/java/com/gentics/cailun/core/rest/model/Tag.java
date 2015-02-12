package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class Tag extends CaiLunNode {

	private static final long serialVersionUID = -4914662176121360429L;

	@Indexed
	protected String name;

	public Tag(String name) {
		this.name = name;
	}
}
