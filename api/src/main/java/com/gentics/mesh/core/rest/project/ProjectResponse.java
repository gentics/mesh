package com.gentics.mesh.core.rest.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

public class ProjectResponse extends AbstractGenericRestResponse {

	@JsonPropertyDescription("Name of the project")
	private String name;

	@JsonPropertyDescription("Uuid of the project root node. All futher nodes are children of this node.")
	private String rootNodeUuid;

	public ProjectResponse() {
	}

	/**
	 * Return name of the project.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set name of the project.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the root node uuid of the project.
	 * 
	 * @return
	 */
	public String getRootNodeUuid() {
		return rootNodeUuid;
	}

	/**
	 * Set the root node uuid of the project.
	 * 
	 * @param rootNodeUuid
	 */
	public void setRootNodeUuid(String rootNodeUuid) {
		this.rootNodeUuid = rootNodeUuid;
	}

}
