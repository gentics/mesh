package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

public class StringGraphFieldListImpl extends AbstractBasicGraphFieldList<StringGraphField, StringFieldListImpl> implements StringGraphFieldList {

	public static void checkIndices(Database database) {
		database.addVertexType(StringGraphFieldListImpl.class);
	}

	@Override
	public StringGraphField createString(String string) {
		StringGraphField field = createField();
		field.setString(string);
		return field;
	}

	@Override
	public StringGraphField getString(int index) {
		return getField(index);
	}

	@Override
	protected StringGraphField createField(String key) {
		return new StringGraphFieldImpl(key, getImpl());
	}

	@Override
	public Class<? extends StringGraphField> getListType() {
		return StringGraphFieldImpl.class;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub
	}

	@Override
	public Observable<StringFieldListImpl> transformToRest(InternalActionContext ac, String fieldKey) {
		StringFieldListImpl restModel = new StringFieldListImpl();
		for (StringGraphField item : getList()) {
			restModel.add(item.getString());
		}
		return Observable.just(restModel);
	}

}
