package com.gentics.mesh.core.data.model.node.field;

import com.gentics.mesh.core.data.model.node.MeshNodeFieldContainer;

public class StringFieldProperty extends AbstractSimpleFieldProperty {

	public StringFieldProperty(String key, MeshNodeFieldContainer parentContainer) {
		super(key, parentContainer);
	}

	public void setString(String string) {
		setFieldProperty("string", string);
	}

	public String getString() {
		return getFieldProperty("string");
	}

}
