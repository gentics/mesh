package com.gentics.mesh.check;

import com.gentics.mesh.core.rest.common.FieldTypes;

/**
 * Test for checking consistency of "jsonlistitem"
 */
public class JsonListItemCheck  extends AbstractListItemTableCheck {

	@Override
	public String getName() {
		return "jsonlistitem";
	}

	@Override
	protected FieldTypes getListFieldType() {
		return FieldTypes.JSON;
	}
}
