package com.gentics.mesh.check;

import com.gentics.mesh.core.rest.common.FieldTypes;

/**
 * Test for checking consistency of "boollistitem"
 */
public class BoolListItemCheck extends AbstractListItemTableCheck {
	@Override
	public String getName() {
		return "boollistitem";
	}

	@Override
	protected FieldTypes getListFieldType() {
		return FieldTypes.BOOLEAN;
	}
}
