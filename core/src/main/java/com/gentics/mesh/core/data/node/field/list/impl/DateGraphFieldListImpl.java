package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.impl.basic.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class DateGraphFieldListImpl extends AbstractBasicGraphFieldList<DateGraphField, DateFieldListImpl>implements DateGraphFieldList {

	@Override
	public DateGraphField createDate(Long date) {
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
	public void transformToRest(InternalActionContext ac, String fieldKey, Handler<AsyncResult<DateFieldListImpl>> handler) {
		DateFieldListImpl restModel = new DateFieldListImpl();
		for (DateGraphField item : getList()) {
			restModel.add(item.getDate());
		}
		handler.handle(Future.succeededFuture(restModel));
	}
}
