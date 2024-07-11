package com.gentics.mesh.core.rest.node.field.list.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.MicronodeFieldModel;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldListModel;

/**
 * REST model for a micronode list field. Please note that {@link FieldMap} will handle the actual JSON format building.
 */
public class MicronodeFieldListImpl extends AbstractFieldList<MicronodeFieldModel> implements MicronodeFieldListModel {

	@Override
	public String getItemType() {
		return FieldTypes.MICRONODE.toString();
	}
}
