package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.service.SchemaStorage.getSchemaStorage;

import java.io.IOException;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.json.JsonUtil;

public class SchemaContainerImpl extends AbstractGenericNode implements SchemaContainer {

	//	public SchemaResponse transformToRest(MeshAuthUser user) {
	//		////		Collections.sort(schemaForRest.getPropertyTypeSchemas(), new PropertTypeSchemaComparator());
	//		//		// Sort the property types schema. Otherwise rest response is erratic
	//		//
	//		//		for (ProjectImpl project : getProjects()) {
	//		//			// project = neo4jTemplate.fetch(project);
	//		//			ProjectResponse restProject = new ProjectResponse();
	//		//			restProject.setUuid(project.getUuid());
	//		//			restProject.setName(project.getName());
	//		//			schemaForRest.getProjects().add(restProject);
	//		//		}
	//		//		// Sort the list by project name
	//		//		Collections.sort(schemaForRest.getProjects(), new Comparator<ProjectResponse>() {
	//		//			@Override
	//		//			public int compare(ProjectResponse o1, ProjectResponse o2) {
	//		//				return o1.getName().compareTo(o2.getName());
	//		//			};
	//		//		});
	//		//		return schemaForRest;
	//		return null;
	//	}

	@Override
	public void delete() {
		//TODO should all references be updated to a new fallback schema?
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
		Schema schema = getSchemaStorage().getSchema(getSchemaName());
		if (schema == null) {
			schema = JsonUtil.readSchema(getJson());
			getSchemaStorage().addSchema(schema);
		}
		return schema;

	}

	@Override
	public void setSchema(Schema schema) {
		getSchemaStorage().removeSchema(schema.getName());
		getSchemaStorage().addSchema(schema);
		String json = JsonUtil.toJson(schema);
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
