package com.gentics.mesh.core.rest.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.user.NodeReference;

/**
 * REST model for a project response.
 */
public class ProjectResponse extends AbstractGenericRestResponse {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The name of the project.")
	private String name;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The project root node. All futher nodes are children of this node.")
	private NodeReference rootNode;

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
	public NodeReference getRootNode() {
		return rootNode;
	}

	/**
	 * Set the root node uuid of the project.
	 * 
	 * @param rootNode
	 */
	public void setRootNode(NodeReference rootNode) {
		this.rootNode = rootNode;
	}

}
