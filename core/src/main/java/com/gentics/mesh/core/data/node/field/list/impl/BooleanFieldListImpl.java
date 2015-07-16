package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.basic.BooleanField;
import com.gentics.mesh.core.data.node.field.impl.basic.BooleanFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicFieldList;
import com.gentics.mesh.core.data.node.field.list.BooleanFieldList;

public class BooleanFieldListImpl extends AbstractBasicFieldList<BooleanField> implements BooleanFieldList {

	@Override
	public BooleanField getBoolean(String key) {
		return getField(key);
	}

	@Override
	public BooleanField createBoolean(String flag) {
		BooleanField field = createField();
		field.setBoolean(Boolean.valueOf(flag));
		return field;
	}

	@Override
	protected BooleanField createField(String key) {
		return new BooleanFieldImpl(key, getImpl());
	}

}
