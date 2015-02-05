package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

@NodeEntity
public class GenericProject extends AbstractPersistable {

	@Fetch
	String name;

	@Fetch
	@RelatedTo(type = "ASSIGNED_TO_PROJECT", direction = Direction.OUTGOING, elementClass = Tag.class)
	private Set<Tag> childRootTags = new HashSet<>();

}
