package com.gentics.mesh.core.data.model.node.field;

import com.gentics.mesh.core.data.model.node.MeshNodeFieldContainer;

public class StringField extends AbstractSimpleField {

	public StringField(String key, MeshNodeFieldContainer parentContainer) {
		super(key, parentContainer);
	}

	public void setString(String string) {
		setFieldProperty("string", string);
	}

	public String getString() {
		return getFieldProperty("string");
	}

}
