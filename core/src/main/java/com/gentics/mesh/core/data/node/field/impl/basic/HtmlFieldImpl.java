package com.gentics.mesh.core.data.node.field.impl.basic;

import com.gentics.mesh.core.data.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.basic.HtmlField;
import com.syncleus.ferma.AbstractVertexFrame;

public class HtmlFieldImpl extends AbstractBasicField implements HtmlField {

	public HtmlFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setHTML(String html) {
		setFieldProperty("html", html);
	}

	public String getHTML() {
		return getFieldProperty("html");
	}

}
