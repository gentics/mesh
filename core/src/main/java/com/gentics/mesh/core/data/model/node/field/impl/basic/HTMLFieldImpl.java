package com.gentics.mesh.core.data.model.node.field.impl.basic;

import com.gentics.mesh.core.data.model.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.basic.AbstractBasicField;
import com.gentics.mesh.core.data.model.node.field.basic.HTMLField;

public class HTMLFieldImpl extends AbstractBasicField implements HTMLField {

	public HTMLFieldImpl(String fieldKey, AbstractFieldContainerImpl parentContainer) {
		super(fieldKey, parentContainer);
	}

	public void setHTML(String html) {
		setFieldProperty("html", html);
	}

	public String getHTML() {
		return getFieldProperty("html");
	}

}
