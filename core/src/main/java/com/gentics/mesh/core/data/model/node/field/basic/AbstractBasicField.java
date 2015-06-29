package com.gentics.mesh.core.data.model.node.field.basic;

import com.gentics.mesh.core.data.model.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.Field;

public abstract class AbstractBasicField implements Field {

	private String fieldKey;
	private AbstractFieldContainerImpl parentContainer;

	public AbstractBasicField(String fieldKey, AbstractFieldContainerImpl parentContainer) {
		this.fieldKey = fieldKey;
		this.parentContainer = parentContainer;
	}

	@Override
	public String getFieldLabel() {
		return getFieldProperty("label");
	}

	@Override
	public void setFieldLabel(String label) {
		setFieldProperty("label", label);
	}

	@Override
	public String getFieldName() {
		return getFieldProperty("name");
	}

	@Override
	public void setFieldName(String name) {
		setFieldProperty("name", name);
	}

	@Override
	public String getFieldKey() {
		return fieldKey;
	}

	@Override
	public void setFieldKey(String key) {
		setFieldProperty("field", "true");
	}

	public AbstractFieldContainerImpl getParentContainer() {
		return parentContainer;
	}

	public void setFieldProperty(String key, String value) {
		parentContainer.setProperty(fieldKey + "-" + key, value);
	}

	public String getFieldProperty(String key) {
		return parentContainer.getProperty(fieldKey + "-" + key);
	}

}
