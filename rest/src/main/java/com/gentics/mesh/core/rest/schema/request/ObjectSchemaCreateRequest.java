package com.gentics.mesh.core.rest.schema.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ObjectSchemaCreateRequest extends ObjectSchemaUpdateRequest {

	@JsonIgnore
	private String uuid;

	private String projectUuid;
	
	public ObjectSchemaCreateRequest() {
	}

	public String getProjectUuid() {
		return projectUuid;
	}

	public void setProjectUuid(String projectUuid) {
		this.projectUuid = projectUuid;
	}

}
