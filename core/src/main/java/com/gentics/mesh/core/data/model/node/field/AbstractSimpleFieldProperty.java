package com.gentics.mesh.core.data.model.node.field;

import com.gentics.mesh.core.data.model.node.MeshNodeFieldContainer;

public abstract class AbstractSimpleFieldProperty {

	private String fieldKey;
	private MeshNodeFieldContainer parentContainer;

	public AbstractSimpleFieldProperty(String fieldKey, MeshNodeFieldContainer parentContainer) {
		this.fieldKey = fieldKey;
		this.parentContainer = parentContainer;
	}

	public String getLabel() {
		return getFieldProperty("label");
	}

	public void setLabel(String label) {
		setFieldProperty("label", label);
	}

	public String getName() {
		return getFieldProperty("name");
	}

	public void setName(String name) {
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
