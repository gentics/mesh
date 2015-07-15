package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.basic.NumberField;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberFieldList;

public class NumberFieldListImpl extends AbstractBasicFieldList<NumberField> implements NumberFieldList{

	@Override
	public NumberField createNumber(String key) {
//		addItem(item);
		return null;
	}

	@Override
	protected NumberField convertBasicValue(String listItemValue) {
		// TODO Auto-generated method stub
		return null;
	}

}
