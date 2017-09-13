package com.gentics.mesh.core.rest.schema;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;

public class ReleaseInfoMicroschemaList implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("List of microschema references.")
	@JsonDeserialize(contentAs = MicroschemaReferenceImpl.class)
	private List<MicroschemaReference> microschemas = new ArrayList<>();

	/**
	 * Create an empty list
	 */
	public ReleaseInfoMicroschemaList() {
	}

	public List<MicroschemaReference> getMicroschemas() {
		return microschemas;
	}

}
