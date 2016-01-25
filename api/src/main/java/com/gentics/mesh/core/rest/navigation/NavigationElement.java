package com.gentics.mesh.core.rest.navigation;

import java.util.List;

import com.gentics.mesh.core.rest.node.NodeResponse;

/**
 * A navigation element is a reference to a node within the navigation tree.
 *
 */
public class NavigationElement {

	private String uuid;

	private NodeResponse node;

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
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
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
	 */
	public void setChildren(List<NavigationElement> children) {
		this.children = children;
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
	 */
	public void setNode(NodeResponse node) {
		this.node = node;
	}

}
