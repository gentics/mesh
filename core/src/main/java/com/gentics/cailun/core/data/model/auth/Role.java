package com.gentics.cailun.core.data.model.auth;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.data.model.generic.GenericNode;

@NodeEntity
public class Role extends GenericNode {

	private static final long serialVersionUID = -6696156556292877992L;

	private String name;

	@SuppressWarnings("unused")
	private Role() {
	}

	public Role(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
