package com.gentics.cailun.core.data.model;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.data.model.generic.GenericNode;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;

/**
 * The object schema is used for validating CRUD actions and to provide a JSON schema that can be used for client side validation.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class ObjectSchema extends GenericNode {

	private static final long serialVersionUID = -6822013445735068604L;

	@Indexed(unique = true)
	private String name;

	// TODO i18n?
	private String description;

	@Fetch
	@RelatedTo(direction = Direction.OUTGOING, elementClass = PropertyTypeSchema.class, type = BasicRelationships.HAS_PROPERTY_TYPE_SCHEMA)
	private Set<PropertyTypeSchema> propertyTypeSchemas = new HashSet<>();

	@SuppressWarnings("unused")
	private ObjectSchema() {
	}

	public ObjectSchema(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public Set<PropertyTypeSchema> getPropertyTypeSchemas() {
		return propertyTypeSchemas;
	}

	public PropertyTypeSchema getPropertyTypeSchema(String typeKey) {
		if (StringUtils.isEmpty(typeKey)) {
			return null;
		}
		for (PropertyTypeSchema propertyTypeSchema : propertyTypeSchemas) {
			if (propertyTypeSchema.getKey().equals(typeKey)) {
				return propertyTypeSchema;
			}
		}
		return null;
	}

	public void setPropertyTypeSchemas(Set<PropertyTypeSchema> propertyTypeSchemas) {
		this.propertyTypeSchemas = propertyTypeSchemas;
	}

	public void addPropertyTypeSchema(PropertyTypeSchema typeSchema) {
		this.propertyTypeSchemas.add(typeSchema);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
