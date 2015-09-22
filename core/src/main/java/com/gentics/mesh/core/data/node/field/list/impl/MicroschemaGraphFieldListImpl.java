package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.impl.nesting.MicroschemaGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicroschemaGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.MicroschemaFieldListImpl;
import com.gentics.mesh.handler.InternalActionContext;

public class MicroschemaGraphFieldListImpl extends AbstractReferencingGraphFieldList<MicroschemaGraphField, MicroschemaFieldListImpl>
		implements MicroschemaGraphFieldList {

	@Override
	public Class<? extends MicroschemaGraphField> getListType() {
		return MicroschemaGraphFieldImpl.class;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}

	@Override
	public MicroschemaFieldListImpl transformToRest(InternalActionContext ac, String fieldKey) {
		// TODO Auto-generated method stub
		return null;
	}

}
