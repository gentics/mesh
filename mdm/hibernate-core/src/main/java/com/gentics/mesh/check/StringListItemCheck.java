package com.gentics.mesh.check;

import com.gentics.mesh.core.rest.common.FieldTypes;

/**
 * Test for checking consistency of "stringlistitem"
 */
public class StringListItemCheck extends AbstractListItemTableCheck {
	@Override
	public String getName() {
		return "stringlistitem";
	}

	@Override
	protected FieldTypes getListFieldType() {
		return FieldTypes.STRING;
	}
}
