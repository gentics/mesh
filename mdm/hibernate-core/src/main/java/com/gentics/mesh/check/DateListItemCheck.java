package com.gentics.mesh.check;

import com.gentics.mesh.core.rest.common.FieldTypes;

/**
 * Test for checking consistency of "datelistitem"
 */
public class DateListItemCheck extends AbstractListItemTableCheck {
	@Override
	public String getName() {
		return "datelistitem";
	}

	@Override
	protected FieldTypes getListFieldType() {
		return FieldTypes.DATE;
	}
}
