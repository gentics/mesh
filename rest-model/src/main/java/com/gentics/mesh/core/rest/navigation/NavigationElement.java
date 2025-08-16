package com.gentics.mesh.core.rest.navigation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.NodeResponse;

/**
 * A navigation element is a reference to a node within the navigation tree.
 */
public class NavigationElement implements RestModel {

	@JsonPropertyDescription("Uuid of the node within this navigation element.")
	private String uuid;

	@JsonPropertyDescription("Detailed node information.")
	private NodeResponse node;

	@JsonPropertyDescription("List of further child elements of the node.")
	private List<NavigationElement> children;

	public NavigationElement() {
	}

	/**
	 * Return the uuid of the navigation element.
	 * 
	 * @return
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the element uuid.
	 * 
	 * @param uuid
	 * @return Fluent API
	 */
	public NavigationElement setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	/**
	 * Return the children of the current element.
	 * 
	 * @return List of elements or null when either no children could be found or the maximum navigation depth has been reached
	 */
	public List<NavigationElement> getChildren() {
		return children;
	}

	/**
	 * Set the navigation children.
	 * 
	 * @param children
	 * @return Fluent API
	 */
	public NavigationElement setChildren(List<NavigationElement> children) {
		this.children = children;
		return this;
	}

	/**
	 * Return the node response object for this navigation element.
	 * 
	 * @return
	 */
	public NodeResponse getNode() {
		return node;
	}

	/**
	 * Set the node response object for this navigation element.
	 * 
	 * @param node
	 * @return Fluent API
	 */
	public NavigationElement setNode(NodeResponse node) {
		this.node = node;
		return this;
	}

}
