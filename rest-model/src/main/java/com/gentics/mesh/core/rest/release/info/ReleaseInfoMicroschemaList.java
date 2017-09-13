package com.gentics.mesh.core.rest.release.info;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;

public class ReleaseInfoMicroschemaList implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("List of microschema references.")
	private List<ReleaseMicroschemaInfo> microschemas = new ArrayList<>();

	/**
	 * Create an empty list
	 */
	public ReleaseInfoMicroschemaList() {
	}

	public List<ReleaseMicroschemaInfo> getMicroschemas() {
		return microschemas;
	}

	
	/**
	 * Add all provided references to the list
	 * 
	 * @param references
	 * @return Fluent API
	 */
	public ReleaseInfoMicroschemaList add(MicroschemaReference... references) {
		for (MicroschemaReference reference : references) {
			getMicroschemas().add(new ReleaseMicroschemaInfo(reference));
		}
		return this;
	}
}
