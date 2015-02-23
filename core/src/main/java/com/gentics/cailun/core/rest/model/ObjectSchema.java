package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.generic.GenericNode;
import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

/**
 * The object schema is used for validating crud actions and to provide a json schema that can be used for client side validation.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class ObjectSchema extends GenericNode {

	private static final long serialVersionUID = -6822013445735068604L;

	@Fetch
	private String name;

	@Fetch
	// TODO i18n?
	private String description;

	@Fetch
	// TODO multiple types?
	private String type;

//	@JsonIgnore
	@RelatedTo(direction = Direction.OUTGOING, elementClass = Project.class, type = BasicRelationships.ASSIGNED_TO_PROJECT)
	private Project project;

	@RelatedTo(direction = Direction.OUTGOING, elementClass = PropertySchema.class, type = BasicRelationships.HAS_PROPERTY_TYPE)
	private Set<PropertySchema> propertyTypes = new HashSet<>();

	@SuppressWarnings("unused")
	private ObjectSchema() {
	}

	public ObjectSchema(String name) {
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

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

}
