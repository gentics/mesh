package com.gentics.mesh.core.data.impl;

import java.io.IOException;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.response.SchemaResponse;
import com.gentics.mesh.util.JsonUtils;

public class SchemaContainerImpl extends AbstractGenericNode implements SchemaContainer {

	@Override
	public SchemaResponse transformToRest(MeshAuthUser user) {
		//
		//		SchemaResponse schemaForRest = new SchemaResponse();
		////		schemaForRest.setDescription(getDescription());
		////		schemaForRest.setDisplayName(getDisplayName());
		////		schemaForRest.setName(getName());
		//		schemaForRest.setUuid(getUuid());
		//		// TODO creator
		//
		//		// TODO we need to add checks that prevents multiple schemas with the same key
		//		for (BasicPropertyType propertyTypeSchema : getPropertyTypes()) {
		//			// propertyTypeSchema = neo4jTemplate.fetch(propertyTypeSchema);
		////			PropertyTypeSchemaResponse propertyTypeSchemaForRest = new PropertyTypeSchemaResponse();
		////			propertyTypeSchemaForRest.setUuid(propertyTypeSchema.getUuid());
		////			propertyTypeSchemaForRest.setKey(propertyTypeSchema.getKey());
		////			propertyTypeSchemaForRest.setDescription(propertyTypeSchema.getDescription());
		////			propertyTypeSchemaForRest.setType(propertyTypeSchema.getType());
		////			propertyTypeSchemaForRest.setDisplayName(propertyTypeSchema.getDisplayName());
		////			schemaForRest.getPropertyTypeSchemas().add(propertyTypeSchemaForRest);
		//		}
		////		Collections.sort(schemaForRest.getPropertyTypeSchemas(), new PropertTypeSchemaComparator());
		//		// Sort the property types schema. Otherwise rest response is erratic
		//
		//		for (ProjectImpl project : getProjects()) {
		//			// project = neo4jTemplate.fetch(project);
		//			ProjectResponse restProject = new ProjectResponse();
		//			restProject.setUuid(project.getUuid());
		//			restProject.setName(project.getName());
		//			schemaForRest.getProjects().add(restProject);
		//		}
		//		// Sort the list by project name
		//		Collections.sort(schemaForRest.getProjects(), new Comparator<ProjectResponse>() {
		//			@Override
		//			public int compare(ProjectResponse o1, ProjectResponse o2) {
		//				return o1.getName().compareTo(o2.getName());
		//			};
		//		});
		//		return schemaForRest;
		return null;
	}

	@Override
	public void delete() {
		getElement().remove();
	}

	@Override
	public SchemaContainerImpl getImpl() {
		return this;
	}

	private String getJson() {
		return getProperty("json");
	}

	private void setJson(String json) {
		setProperty("json", json);
	}

	@Override
	public Schema getSchema() throws IOException {
		return JsonUtils.readValue(getJson(), SchemaImpl.class);
	}

	@Override
	public void setSchema(Schema schema) {
		String json = JsonUtils.toJson(schema);
		setJson(json);
	}

	@Override
	public void setSchemaName(String name) {
		setProperty("name", name);
	}

	@Override
	public String getSchemaName() {
		return getProperty("name");
	}

}
