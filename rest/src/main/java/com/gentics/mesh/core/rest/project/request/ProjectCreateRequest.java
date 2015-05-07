package com.gentics.mesh.core.rest.project.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProjectCreateRequest extends ProjectUpdateRequest {

	@JsonIgnore
	private String uuid;
}
