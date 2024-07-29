package com.gentics.mesh.core.rest.node.field.list.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;

/**
 * REST model for a micronode list field. Please note that {@link FieldMap} will handle the actual JSON format building.
 */
public class MicronodeFieldListImpl extends AbstractFieldList<MicronodeField> implements MicronodeFieldList {

	@Override
	public String getItemType() {
		return FieldTypes.MICRONODE.toString();
	}
}
