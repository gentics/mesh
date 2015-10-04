package com.gentics.mesh.core.data.node.field.impl.basic;

import com.gentics.mesh.core.data.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.syncleus.ferma.AbstractVertexFrame;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class HtmlGraphFieldImpl extends AbstractBasicField<HtmlField>implements HtmlGraphField {

	public HtmlGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setHtml(String html) {
		setFieldProperty("html", html);
	}

	@Override
	public String getHTML() {
		return getFieldProperty("html");
	}

	@Override
	public void transformToRest(ActionContext ac, Handler<AsyncResult<HtmlField>> handler) {
		HtmlFieldImpl htmlField = new HtmlFieldImpl();
		String html = getHTML();
		//TODO really empty string for unset field value?!
		htmlField.setHTML(html == null ? "" : html);
		handler.handle(Future.succeededFuture(htmlField));
	}

}
