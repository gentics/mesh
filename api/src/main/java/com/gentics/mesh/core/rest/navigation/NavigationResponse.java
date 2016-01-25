package com.gentics.mesh.core.rest.navigation;

import com.gentics.mesh.core.rest.common.RestModel;

public class NavigationResponse implements RestModel {

	private NavigationElement root = new NavigationElement();

	/**
	 * Return the navigation root element.
	 * 
	 * @return
	 */
	public NavigationElement getRoot() {
		return root;
	}

	/**
	 * Set the navigation structure.
	 * 
	 * @param root
	 */
	public void setRoot(NavigationElement root) {
		this.root = root;
	}
}
