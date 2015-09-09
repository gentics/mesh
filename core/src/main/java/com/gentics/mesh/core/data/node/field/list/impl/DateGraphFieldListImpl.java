package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.impl.basic.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.handler.ActionContext;

public class DateGraphFieldListImpl extends AbstractBasicGraphFieldList<DateGraphField, DateFieldListImpl>implements DateGraphFieldList {

	@Override
	public DateGraphField createDate(String date) {
		DateGraphField field = createField();
		field.setDate(date);
		return field;
	}

	@Override
	protected DateGraphField createField(String key) {
		return new DateGraphFieldImpl(key, getImpl());
	}

	@Override
	public DateGraphField getDate(int index) {
		return getField(index);
	}

	@Override
	public Class<? extends DateGraphField> getListType() {
		return DateGraphFieldImpl.class;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub
	}

	@Override
	public DateFieldListImpl transformToRest(ActionContext ac, String fieldKey) {
		DateFieldListImpl restModel = new DateFieldListImpl();
		for (DateGraphField item : getList()) {
			restModel.add(item.getDate());
		}
		return restModel;
	}
}
