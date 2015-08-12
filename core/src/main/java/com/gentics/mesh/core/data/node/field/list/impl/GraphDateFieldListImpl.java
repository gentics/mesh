package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.impl.basic.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphDateFieldList;

public class GraphDateFieldListImpl extends AbstractBasicGraphFieldList<DateGraphField> implements GraphDateFieldList {

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
}
