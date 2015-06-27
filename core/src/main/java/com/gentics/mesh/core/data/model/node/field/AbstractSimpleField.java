package com.gentics.mesh.core.data.model.node.field;

import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;

public abstract class AbstractSimpleField implements Field {

	private String fieldKey;
	private MeshNodeFieldContainerImpl parentContainer;

	public AbstractSimpleField(String fieldKey, MeshNodeFieldContainerImpl parentContainer) {
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

	public MeshNodeFieldContainerImpl getParentContainer() {
		return parentContainer;
	}

	public void setFieldProperty(String key, String value) {
		parentContainer.setProperty(fieldKey + "-" + key, value);
	}

	public String getFieldProperty(String key) {
		return parentContainer.getProperty(fieldKey + "-" + key);
	}
}
