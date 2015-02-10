package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

@NodeEntity
public class CaiLunRoot extends AbstractPersistable {

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_PROJECT, direction = Direction.OUTGOING, elementClass = Project.class)
	Set<Project> projects = new HashSet<>();
}
