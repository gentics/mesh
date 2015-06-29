package com.gentics.mesh.core.data.model.node.field.nesting;

import com.gentics.mesh.core.data.model.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.Field;

public abstract class AbstractComplexField extends AbstractFieldContainerImpl implements Field {

	@Override
	public String getFieldLabel() {
		return getProperty("label");
	}

	@Override
	public void setFieldLabel(String label) {
		setProperty("label", label);
	}

	@Override
	public String getFieldName() {
		return getProperty("name");
	}

	@Override
	public void setFieldName(String name) {
		setProperty("name", name);
	}

	@Override
	public String getFieldKey() {
		return getProperty("fieldKey");
	}

	@Override
	public void setFieldKey(String key) {
		setProperty("fieldKey", key);
	}

}
