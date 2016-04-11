package com.gentics.mesh.core.data.node.field.impl;

import java.util.Objects;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.syncleus.ferma.AbstractVertexFrame;

import rx.Observable;

public class HtmlGraphFieldImpl extends AbstractBasicField<HtmlField> implements HtmlGraphField {

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
	public Observable<HtmlField> transformToRest(ActionContext ac) {
		HtmlFieldImpl htmlField = new HtmlFieldImpl();
		String html = getHTML();
		//TODO really empty string for unset field value?!
		htmlField.setHTML(html == null ? "" : html);
		return Observable.just(htmlField);
	}

	@Override
	public void removeField(GraphFieldContainer container) {
		setFieldProperty("html", null);
		setFieldKey(null);
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		HtmlGraphField clone = container.createHTML(getFieldKey());
		clone.setHtml(getHTML());
		return clone;
	}

	@Override
	public boolean equals(GraphField field) {
		if (field instanceof HtmlGraphField) {
			String htmlA = getHTML();
			String htmlB = ((HtmlGraphField) field).getHTML();
			return Objects.equals(htmlA, htmlB);
		}
		return false;
	}

	@Override
	public boolean equals(Field restField) {
		if (restField instanceof HtmlField) {
			String htmlA = getHTML();
			String htmlB = ((HtmlField) restField).getHTML();
			return Objects.equals(htmlA, htmlB);
		}
		return false;
	}
}
