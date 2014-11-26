package com.gentics.vertx.cailun.repository;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class Tag implements TagableContent {
	
	@Indexed
	String name;

	@GraphId
	Long id;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
