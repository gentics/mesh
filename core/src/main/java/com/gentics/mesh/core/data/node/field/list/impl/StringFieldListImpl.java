package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.basic.StringField;
import com.gentics.mesh.core.data.node.field.impl.basic.StringFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicFieldList;
import com.gentics.mesh.core.data.node.field.list.StringFieldList;

public class StringFieldListImpl extends AbstractBasicFieldList<StringField> implements StringFieldList {

	@Override
	public StringField createString(String string) {
		StringField field = createField();
		field.setString(string);
		return field;
	}

	@Override
	public StringField getString(String key) {
		return getField(key);
	}

	@Override
	protected StringField createField(String key) {
		return new StringFieldImpl(key, getImpl());
	}

	@Override
	public Class<? extends StringField> getListType() {
		return StringFieldImpl.class;
	}

}
