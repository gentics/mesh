package com.gentics.mesh.core.rest.tag;

import com.gentics.mesh.core.rest.common.AbstractRestModel;

public class TagFamilyUpdateRequest extends AbstractRestModel {

	private String name;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
