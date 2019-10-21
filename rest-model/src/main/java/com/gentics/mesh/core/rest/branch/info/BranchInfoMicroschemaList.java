package com.gentics.mesh.core.rest.branch.info;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

public class BranchInfoMicroschemaList implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("List of microschema references.")
	private List<BranchMicroschemaInfo> microschemas = new ArrayList<>();

	/**
	 * Create an empty list
	 */
	public BranchInfoMicroschemaList() {
	}

	public BranchInfoMicroschemaList(List<BranchMicroschemaInfo> microschemas) {
		this.microschemas = microschemas;
	}

	public List<BranchMicroschemaInfo> getMicroschemas() {
		return microschemas;
	}

	
	/**
	 * Add all provided references to the list
	 * 
	 * @param references
	 * @return Fluent API
	 */
	public BranchInfoMicroschemaList add(MicroschemaReference... references) {
		for (MicroschemaReference reference : references) {
			getMicroschemas().add(new BranchMicroschemaInfo(reference));
		}
		return this;
	}
}
