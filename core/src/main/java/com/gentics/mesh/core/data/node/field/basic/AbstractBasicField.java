package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.Field;

public abstract class AbstractBasicField implements Field {

	private String fieldKey;
	private AbstractFieldContainerImpl parentContainer;

	public AbstractBasicField(String fieldKey, AbstractFieldContainerImpl parentContainer) {
		this.fieldKey = fieldKey;
		this.parentContainer = parentContainer;
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
