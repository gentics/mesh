package com.gentics.mesh.core.data.model.node.field;

import com.gentics.mesh.core.data.model.node.MeshNodeFieldContainer;

public abstract class AbstractSimpleField implements Field {

	private String fieldKey;
	private MeshNodeFieldContainer parentContainer;

	public AbstractSimpleField(String fieldKey, MeshNodeFieldContainer parentContainer) {
		this.fieldKey = fieldKey;
		this.parentContainer = parentContainer;
	}

	public String getFieldLabel() {
		return getFieldProperty("label");
	}

	public void setFieldLabel(String label) {
		setFieldProperty("label", label);
	}

	public String getFieldName() {
		return getFieldProperty("name");
	}

	public void setFieldName(String name) {
		setFieldProperty("name", name);
	}

	public String getFieldKey() {
		return fieldKey;
	}

	public MeshNodeFieldContainer getParentContainer() {
		return parentContainer;
	}

	public void setFieldProperty(String key, String value) {
		parentContainer.setProperty(fieldKey + "-" + key, value);
	}

	public String getFieldProperty(String key) {
		return parentContainer.getProperty(fieldKey + "-" + key);
	}
}
