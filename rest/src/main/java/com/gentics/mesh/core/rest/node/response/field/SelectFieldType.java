package com.gentics.mesh.core.rest.node.response.field;

import java.util.List;

import com.gentics.mesh.model.FieldTypes;

public class SelectFieldType extends AbstractField {

	//TODO check whether we also want to support nodes in here? Do we want to support tags as well?
	private List<String> options;

	public List<String> getOptions() {
		return options;
	}

	@Override
	public String getType() {
		return FieldTypes.SELECT.toString();
	}
}
