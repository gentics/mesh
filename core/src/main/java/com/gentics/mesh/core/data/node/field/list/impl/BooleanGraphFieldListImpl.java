package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.impl.basic.BooleanGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.handler.ActionContext;

public class BooleanGraphFieldListImpl extends AbstractBasicGraphFieldList<BooleanGraphField, BooleanFieldListImpl>implements BooleanGraphFieldList {

	@Override
	public BooleanGraphField getBoolean(int index) {
		return getField(index);
	}

	@Override
	public BooleanGraphField createBoolean(Boolean flag) {
		BooleanGraphField field = createField();
		field.setBoolean(flag);
		return field;
	}

	@Override
	protected BooleanGraphField createField(String key) {
		return new BooleanGraphFieldImpl(key, getImpl());
	}

	@Override
	public Class<? extends BooleanGraphField> getListType() {
		return BooleanGraphFieldImpl.class;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub
	}

	@Override
	public BooleanFieldListImpl transformToRest(ActionContext ac, String fieldKey) {
		BooleanFieldListImpl restModel = new BooleanFieldListImpl();
		for (BooleanGraphField item : getList()) {
			restModel.add(item.getBoolean());
		}
		return restModel;
	}

}
