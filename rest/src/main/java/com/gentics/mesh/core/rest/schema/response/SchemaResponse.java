package com.gentics.mesh.core.rest.schema.response;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

import com.gentics.mesh.core.rest.common.response.AbstractRestModel;
import com.gentics.mesh.core.rest.project.response.ProjectResponse;

public class SchemaResponse extends AbstractRestModel {

	private final String type = "object";

	@JsonProperty("title")
	private String name;

	private String description;
	private String displayName;

	@JsonProperty("properties")
	private List<PropertyTypeSchemaResponse> propertyTypeSchemas = new ArrayList<>();

	private List<ProjectResponse> projects = new ArrayList<>();

	private String[] perms = {};

	public SchemaResponse() {
	}

	public SchemaResponse(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<PropertyTypeSchemaResponse> getPropertyTypeSchemas() {
		return propertyTypeSchemas;
	}

	public void setPropertyTypeSchemas(List<PropertyTypeSchemaResponse> propertyTypeSchemas) {
		this.propertyTypeSchemas = propertyTypeSchemas;
	}

	public String[] getPerms() {
		return perms;
	}

	public void setPerms(String... perms) {
		this.perms = perms;
	}

	public List<ProjectResponse> getProjects() {
		return projects;
	}

	public void setProjects(List<ProjectResponse> projects) {
		this.projects = projects;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

}
