package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.service.ServerSchemaStorage.getSchemaStorage;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.json.JsonUtil;

public class SchemaContainerImpl extends AbstractGenericNode implements SchemaContainer {

	@Override
	public SchemaResponse transformToRest(MeshAuthUser requestUser) throws JsonParseException, JsonMappingException, IOException {
		SchemaResponse schemaResponse = JsonUtil.readSchema(getJson(), SchemaResponse.class);
		schemaResponse.setUuid(getUuid());

		for (ProjectImpl project : getProjects()) {
			ProjectResponse restProject = new ProjectResponse();
			restProject.setUuid(project.getUuid());
			restProject.setName(project.getName());
			schemaResponse.getProjects().add(restProject);
		}

		// Sort the list by project name
		Collections.sort(schemaResponse.getProjects(), new Comparator<ProjectResponse>() {
			@Override
			public int compare(ProjectResponse o1, ProjectResponse o2) {
				return o1.getName().compareTo(o2.getName());
			};
		});

		schemaResponse.setPermissions(requestUser.getPermissionNames(this));
		
		return schemaResponse;
	}

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
			schema = JsonUtil.readSchema(getJson(), SchemaImpl.class);
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
