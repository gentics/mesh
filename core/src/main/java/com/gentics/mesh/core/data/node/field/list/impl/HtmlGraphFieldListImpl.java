package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.impl.basic.HtmlGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class HtmlGraphFieldListImpl extends AbstractBasicGraphFieldList<HtmlGraphField, HtmlFieldListImpl>implements HtmlGraphFieldList {

	@Override
	public HtmlGraphField createHTML(String html) {
		HtmlGraphField field = createField();
		field.setHtml(html);
		return field;
	}

	@Override
	protected HtmlGraphField createField(String key) {
		return new HtmlGraphFieldImpl(key, getImpl());
	}

	@Override
	public HtmlGraphField getHTML(int index) {
		return getField(index);
	}

	@Override
	public Class<? extends HtmlGraphField> getListType() {
		return HtmlGraphFieldImpl.class;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub
	}

	@Override
	public void transformToRest(InternalActionContext ac, String fieldKey, Handler<AsyncResult<HtmlFieldListImpl>> handler) {
		HtmlFieldListImpl restModel = new HtmlFieldListImpl();
		for (HtmlGraphField item : getList()) {
			restModel.add(item.getHTML());
		}
		handler.handle(Future.succeededFuture(restModel));
	}
}
