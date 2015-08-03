package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.RestModel;


/**
 * Tag create request model.
 *
 */
public class TagFamilyCreateRequest implements RestModel {

	private String name;

	public TagFamilyCreateRequest() {
		// TODO Auto-generated constructor stub
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
