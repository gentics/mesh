package com.gentics.mesh.core.rest.project.request;

import org.codehaus.jackson.annotate.JsonIgnore;

public class ProjectCreateRequest extends ProjectUpdateRequest {

	@JsonIgnore
	private String uuid;
}
