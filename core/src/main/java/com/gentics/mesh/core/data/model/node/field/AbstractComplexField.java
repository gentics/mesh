package com.gentics.mesh.core.data.model.node.field;

import com.gentics.mesh.core.data.model.generic.MeshVertexImpl;

public abstract class AbstractComplexField extends MeshVertexImpl implements Field {

	public String getFieldLabel() {
		return getProperty("label");
	}

	public void setFieldLabel(String label) {
		setProperty("label", label);
	}

	public String getFieldName() {
		return getProperty("name");
	}

	public void setFieldName(String name) {
		setProperty("name", name);
	}

	@Override
	public String getFieldKey() {
		return getProperty("fieldKey");
	}

	public void setFieldKey(String key) {
		setProperty("fieldKey", key);
	}

}
