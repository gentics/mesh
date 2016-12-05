package com.gentics.mesh.core.rest.project;

import com.gentics.mesh.core.rest.common.RestModel;

public class ProjectUpdateRequest implements RestModel {

	private String name;

	public ProjectUpdateRequest() {
	}

	/**
	 * Return the project name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the project name.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

}
