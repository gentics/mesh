package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.impl.nesting.GraphMicroschemaFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphMicroschemaFieldList;
import com.gentics.mesh.core.data.node.field.nesting.GraphMicroschemaField;
import com.gentics.mesh.core.rest.node.field.list.impl.MicroschemaFieldListImpl;
import com.gentics.mesh.handler.ActionContext;

public class GraphMicroschemaFieldListImpl extends AbstractReferencingGraphFieldList<GraphMicroschemaField, MicroschemaFieldListImpl>
		implements GraphMicroschemaFieldList {

	@Override
	public Class<? extends GraphMicroschemaField> getListType() {
		return GraphMicroschemaFieldImpl.class;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}

	@Override
	public MicroschemaFieldListImpl transformToRest(ActionContext ac, String fieldKey) {
		// TODO Auto-generated method stub
		return null;
	}

}
