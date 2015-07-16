package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.basic.NumberField;
import com.gentics.mesh.core.data.node.field.impl.basic.NumberFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberFieldList;

public class NumberFieldListImpl extends AbstractBasicFieldList<NumberField> implements NumberFieldList {

	@Override
	public NumberField createNumber(String number) {
		NumberField field = createField();
		field.setNumber(number);
		return field;
	}

	@Override
	public NumberField getNumber(String key) {
		return getField(key);
	}

	@Override
	protected NumberField createField(String key) {
		return new NumberFieldImpl(key, getImpl());
	}
	
	@Override
	public Class<? extends NumberField> getListType() {
		return NumberFieldImpl.class;
	}

}
