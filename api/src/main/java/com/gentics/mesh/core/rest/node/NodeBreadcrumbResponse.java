package com.gentics.mesh.core.rest.node;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Rest model POJO for a node breadcrumb response.
 */
public class NodeBreadcrumbResponse implements RestModel {

	private String name = "test";

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
