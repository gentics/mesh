package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.impl.BooleanGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

/**
 * @see BooleanGraphFieldList
 */
public class BooleanGraphFieldListImpl extends AbstractBasicGraphFieldList<BooleanGraphField, BooleanFieldListImpl> implements BooleanGraphFieldList {

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
	public Observable<BooleanFieldListImpl> transformToRest(InternalActionContext ac, String fieldKey) {
		BooleanFieldListImpl restModel = new BooleanFieldListImpl();
		for (BooleanGraphField item : getList()) {
			restModel.add(item.getBoolean());
		}
		return Observable.just(restModel);
	}

}
