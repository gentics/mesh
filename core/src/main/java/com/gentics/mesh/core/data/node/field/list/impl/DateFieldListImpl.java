package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.basic.DateField;
import com.gentics.mesh.core.data.node.field.impl.basic.DateFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicFieldList;
import com.gentics.mesh.core.data.node.field.list.DateFieldList;

public class DateFieldListImpl extends AbstractBasicFieldList<DateField> implements DateFieldList {

	@Override
	public DateField createDate(String date) {
		DateField field = createField();
		field.setDate(date);
		return field;
	}

	@Override
	protected DateField createField(String key) {
		return new DateFieldImpl(key, getImpl());
	}

	@Override
	public Class<? extends DateField> getListType() {
		return DateFieldImpl.class;
	}
}
