package com.gentics.cailun.core.rest.schema.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ObjectSchemaCreateRequest extends ObjectSchemaUpdateRequest {

	@JsonIgnore
	private String uuid;

}
