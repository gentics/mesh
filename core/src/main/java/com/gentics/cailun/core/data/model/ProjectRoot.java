package com.gentics.cailun.core.data.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.data.model.generic.AbstractPersistable;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;

@NodeEntity
public class ProjectRoot extends AbstractPersistable {

	private static final long serialVersionUID = -761823744502805489L;

	@RelatedTo(type = BasicRelationships.HAS_PROJECT, direction = Direction.OUTGOING, elementClass = Project.class)
	private Set<Project> projects = new HashSet<>();

	@Indexed(unique = true)
	private String unique = ProjectRoot.class.getSimpleName();

	public ProjectRoot() {
	}

	public Set<Project> getProjects() {
		return projects;
	}

}
