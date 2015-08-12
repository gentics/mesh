package com.gentics.mesh.core.data.node.field.impl.basic;

import com.gentics.mesh.core.data.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.syncleus.ferma.AbstractVertexFrame;

public class HtmlGraphFieldImpl extends AbstractBasicField implements HtmlGraphField {

	public HtmlGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setHtml(String html) {
		setFieldProperty("html", html);
	}

	public String getHTML() {
		return getFieldProperty("html");
	}

}
