package com.gentics.mesh.core.data.model.node.field.impl;

import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.AbstractSimpleField;
import com.gentics.mesh.core.data.model.node.field.StringField;

public class StringFieldImpl extends AbstractSimpleField implements StringField {

	public StringFieldImpl(String key, MeshNodeFieldContainerImpl parentContainer) {
		super(key, parentContainer);
	}

	public void setString(String string) {
		setFieldProperty("string", string);
	}

	public String getString() {
		return getFieldProperty("string");
	}

}
