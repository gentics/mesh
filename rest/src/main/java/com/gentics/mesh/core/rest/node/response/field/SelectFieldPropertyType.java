package com.gentics.mesh.core.rest.node.response.field;

import java.util.List;

public class SelectFieldPropertyType extends AbstractFieldProperty {

	//TODO check whether we also want to support nodes in here? Do we want to support tags as well?
	private List<String> options;

	public List<String> getOptions() {
		return options;
	}

	@Override
	public String getType() {
		return PropertyFieldTypes.SELECT.toString();
	}
}
