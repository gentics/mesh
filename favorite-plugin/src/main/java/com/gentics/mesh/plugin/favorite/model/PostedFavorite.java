package com.gentics.mesh.plugin.favorite.model;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a posted like
 */
public class PostedFavorite implements RestModel {
	
	private String value;
	

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
