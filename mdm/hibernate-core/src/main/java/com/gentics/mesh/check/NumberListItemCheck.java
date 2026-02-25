package com.gentics.mesh.check;

import com.gentics.mesh.core.rest.common.FieldTypes;

/**
 * Test for checking consistency of "numberlistitem"
 */
public class NumberListItemCheck extends AbstractListItemTableCheck {
	@Override
	public String getName() {
		return "numberlistitem";
	}

	@Override
	protected FieldTypes getListFieldType() {
		return FieldTypes.NUMBER;
	}
}
