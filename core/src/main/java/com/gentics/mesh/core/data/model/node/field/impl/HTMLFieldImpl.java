package com.gentics.mesh.core.data.model.node.field.impl;

import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.AbstractSimpleField;
import com.gentics.mesh.core.data.model.node.field.HTMLField;

public class HTMLFieldImpl extends AbstractSimpleField implements HTMLField {

	public HTMLFieldImpl(String fieldKey, MeshNodeFieldContainerImpl parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setHTML(String html) {
		setFieldProperty("html", html);
	}

	public String getHTML() {
		return getFieldProperty("html");
	}

}
