package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.impl.nesting.MicroschemaFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingFieldList;
import com.gentics.mesh.core.data.node.field.list.MicroschemaFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaField;

public class MicroschemaFieldListImpl extends AbstractReferencingFieldList<MicroschemaField>implements MicroschemaFieldList {

	@Override
	public Class<? extends MicroschemaField> getListType() {
		return MicroschemaFieldImpl.class;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}

}
