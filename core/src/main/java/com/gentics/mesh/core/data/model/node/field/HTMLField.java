package com.gentics.mesh.core.data.model.node.field;

import com.gentics.mesh.core.data.model.node.MeshNodeFieldContainer;

public class HTMLField extends AbstractSimpleField {

	public HTMLField(String fieldKey, MeshNodeFieldContainer parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setHTML(String html) {
		setFieldProperty("html", html);
	}

	public String getHTML() {
		return getFieldProperty("html");
	}

}
