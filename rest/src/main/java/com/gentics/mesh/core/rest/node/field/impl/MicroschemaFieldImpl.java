package com.gentics.mesh.core.rest.node.field.impl;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.MicroschemaField;
import com.gentics.mesh.core.rest.node.field.MicroschemaListableField;

public class MicroschemaFieldImpl implements MicroschemaField {

	private List<MicroschemaListableField> fields = new ArrayList<>();

	@Override
	public String getType() {
		return FieldTypes.MICROSCHEMA.toString();
	}

	@Override
	public List<MicroschemaListableField> getFields() {
		return fields;
	}

}
