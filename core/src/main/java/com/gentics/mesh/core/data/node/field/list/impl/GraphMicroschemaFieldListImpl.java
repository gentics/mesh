package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.impl.nesting.GraphMicroschemaFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphMicroschemaFieldList;
import com.gentics.mesh.core.data.node.field.nesting.GraphMicroschemaField;

public class GraphMicroschemaFieldListImpl extends AbstractReferencingGraphFieldList<GraphMicroschemaField>implements GraphMicroschemaFieldList {

	@Override
	public Class<? extends GraphMicroschemaField> getListType() {
		return GraphMicroschemaFieldImpl.class;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}

}
