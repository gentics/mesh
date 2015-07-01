package com.gentics.mesh.core.rest.node.field.impl;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.SelectField;

public class SelectFieldImpl implements SelectField {

	private List<String> selections = new ArrayList<>();

	@Override
	public List<String> getSelections() {
		return selections;
	}

	@Override
	public void setSelections(List<String> selections) {
		this.selections = selections;
	}

	@Override
	public String getType() {
		return FieldTypes.SELECT.toString();
	}
}
