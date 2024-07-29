package com.gentics.mesh.core.rest.node.field.list.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;

/**
 * REST model for a node list field. Please note that {@link FieldMap} will handle the actual JSON format building.
 */
public class NodeFieldListImpl extends AbstractFieldList<NodeFieldListItem> implements NodeFieldList {

	@Override
	public String getItemType() {
		return FieldTypes.NODE.toString();
	}
}