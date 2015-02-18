package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.generic.AbstractPersistable;
import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

@NodeEntity
public class ObjectType extends AbstractPersistable {

	private static final long serialVersionUID = -6822013445735068604L;

	@Fetch
	private String name;

	@Fetch
	// TODO i18n?
	private String description;

	@Fetch
	//TODO multiple types?
	private String type;

	@RelatedTo(direction = Direction.OUTGOING, elementClass = Project.class, type = BasicRelationships.ASSIGNED_TO_PROJECT)
	private Project project;

	@RelatedTo(direction = Direction.OUTGOING, elementClass = PropertyType.class, type = BasicRelationships.HAS_PROPERTY_TYPE)
	private Set<PropertyType> propertyTypes = new HashSet<>();

	@SuppressWarnings("unused")
	private ObjectType() {
	}

	public ObjectType(String name) {
		this.name = name;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public Project getProject() {
		return project;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
