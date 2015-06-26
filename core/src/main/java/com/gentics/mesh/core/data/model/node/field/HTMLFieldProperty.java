package com.gentics.mesh.core.data.model.node.field;

import com.gentics.mesh.core.data.model.node.MeshNodeFieldContainer;

public class HTMLFieldProperty extends AbstractSimpleFieldProperty {

	public HTMLFieldProperty(String fieldKey, MeshNodeFieldContainer parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setHTML(String html) {
		setFieldProperty("html", html);
	}

	public String getHTML() {
		return getFieldProperty("html");
	}

}
