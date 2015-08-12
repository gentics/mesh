package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.GraphField;

public abstract class AbstractComplexField extends AbstractFieldContainerImpl implements GraphField {

	@Override
	public String getFieldKey() {
		return getProperty(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public void setFieldKey(String key) {
		setProperty(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

}
