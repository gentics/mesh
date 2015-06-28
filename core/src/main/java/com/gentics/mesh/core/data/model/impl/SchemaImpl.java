package com.gentics.mesh.core.data.model.impl;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_PROPERTY_TYPE;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.gentics.mesh.core.data.model.MeshAuthUser;
import com.gentics.mesh.core.data.model.Schema;
import com.gentics.mesh.core.data.model.generic.AbstractGenericNode;
import com.gentics.mesh.core.data.model.schema.propertytype.BasicPropertyType;
import com.gentics.mesh.core.data.model.schema.propertytype.BasicPropertyTypeImpl;
import com.gentics.mesh.core.data.model.schema.propertytype.MicroPropertyType;
import com.gentics.mesh.core.data.model.schema.propertytype.PropertyType;
import com.gentics.mesh.core.data.service.PropertTypeSchemaComparator;
import com.gentics.mesh.core.rest.project.response.ProjectResponse;
import com.gentics.mesh.core.rest.schema.response.PropertyTypeSchemaResponse;
import com.gentics.mesh.core.rest.schema.response.SchemaResponse;

public class SchemaImpl extends AbstractGenericNode implements Schema {

	private boolean hasBinary = false;

	public boolean isNestingAllowed() {
		return getProperty("isNestingAllowed");
	}

	public void setNestingAllowed(boolean flag) {
		setProperty("isNestingAllowed", flag);
	}

	public boolean hasBinary() {
		return hasBinary;
	}

	// TODO add unique index
	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public String getDisplayName() {
		return getProperty("displayName");
	}

	public void setDisplayName(String name) {
		setProperty("displayName", name);
	}

	// TODO i18n?
	public String getDescription() {
		return getProperty("description");
	}

	public void setDescription(String description) {
		setProperty("description", description);
	}

	public List<? extends TranslatedImpl> getI18nTranslations() {
		return outE(HAS_FIELD_CONTAINER).toList(TranslatedImpl.class);
	}

	public List<? extends BasicPropertyType> getPropertyTypes() {
		return out(HAS_PROPERTY_TYPE).toList(BasicPropertyTypeImpl.class);
	}

	// @Adjacency(label = BasicRelationships.HAS_PROPERTY_TYPE_SCHEMA, direction = Direction.OUT)
	public void addPropertyTypeSchema(BasicPropertyType content) {

	}

	public SchemaResponse transformToRest(MeshAuthUser user) {

		SchemaResponse schemaForRest = new SchemaResponse();
		schemaForRest.setDescription(getDescription());
		schemaForRest.setDisplayName(getDisplayName());
		schemaForRest.setName(getName());
		schemaForRest.setUuid(getUuid());
		// TODO creator

		// TODO we need to add checks that prevents multiple schemas with the same key
		for (BasicPropertyType propertyTypeSchema : getPropertyTypes()) {
			// propertyTypeSchema = neo4jTemplate.fetch(propertyTypeSchema);
			PropertyTypeSchemaResponse propertyTypeSchemaForRest = new PropertyTypeSchemaResponse();
			propertyTypeSchemaForRest.setUuid(propertyTypeSchema.getUuid());
			propertyTypeSchemaForRest.setKey(propertyTypeSchema.getKey());
			propertyTypeSchemaForRest.setDescription(propertyTypeSchema.getDescription());
			propertyTypeSchemaForRest.setType(propertyTypeSchema.getType());
			propertyTypeSchemaForRest.setDisplayName(propertyTypeSchema.getDisplayName());
			schemaForRest.getPropertyTypeSchemas().add(propertyTypeSchemaForRest);
		}
		Collections.sort(schemaForRest.getPropertyTypeSchemas(), new PropertTypeSchemaComparator());
		// Sort the property types schema. Otherwise rest response is erratic

		for (ProjectImpl project : getProjects()) {
			// project = neo4jTemplate.fetch(project);
			ProjectResponse restProject = new ProjectResponse();
			restProject.setUuid(project.getUuid());
			restProject.setName(project.getName());
			schemaForRest.getProjects().add(restProject);
		}
		// Sort the list by project name
		Collections.sort(schemaForRest.getProjects(), new Comparator<ProjectResponse>() {
			@Override
			public int compare(ProjectResponse o1, ProjectResponse o2) {
				return o1.getName().compareTo(o2.getName());
			};
		});
		return schemaForRest;
	}

	public BasicPropertyType create(String key, PropertyType type) {
		BasicPropertyType schemaType = getGraph().addFramedVertex(BasicPropertyTypeImpl.class);
		schemaType.setKey(key);
		schemaType.setType(type);
		return schemaType;
	}

	public MicroPropertyType createMicroPropertyTypeSchema(String key) {
		MicroPropertyType type = getGraph().addFramedVertex(MicroPropertyType.class);
		type.setKey(key);
		type.setType(PropertyType.MICROSCHEMA);
		return type;
	}

	public BasicPropertyTypeImpl createBasicPropertyTypeSchema(String key, PropertyType type) {
		BasicPropertyTypeImpl propertType = getGraph().addFramedVertex(BasicPropertyTypeImpl.class);
		propertType.setKey(key);
		propertType.setType(type);
		linkOut(propertType, HAS_PROPERTY_TYPE);
		return propertType;
	}

	public BasicPropertyTypeImpl createListPropertyTypeSchema(String key) {
		BasicPropertyTypeImpl type = getGraph().addFramedVertex(BasicPropertyTypeImpl.class);
		type.setKey(key);
		return type;
	}

	@Override
	public void delete() {
		getElement().remove();
	}

	@Override
	public SchemaImpl getImpl() {
		return this;
	}

}
