package com.gentics.mesh.core.rest.schema.impl;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.schema.SelectFieldSchema;

public class SelectFieldSchemaImpl extends AbstractFieldSchema implements SelectFieldSchema {

	//TODO check whether we also want to support nodes in here? Do we want to support tags as well?
	private List<String> options;

	private List<String> defaultSelections = new ArrayList<>();

	@Override
	public List<String> getOptions() {
		return options;
	}

	@Override
	public void setSelections(List<String> selections) {
		this.defaultSelections = selections;
	}

	@Override
	public List<String> getSelections() {
		return this.defaultSelections;
	}

}
