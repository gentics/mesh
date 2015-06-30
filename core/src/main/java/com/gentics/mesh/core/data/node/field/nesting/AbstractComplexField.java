package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.Field;

public abstract class AbstractComplexField extends AbstractFieldContainerImpl implements Field {

	@Override
	public String getFieldKey() {
		return getProperty("fieldKey");
	}

	@Override
	public void setFieldKey(String key) {
		setProperty("fieldKey", key);
	}

}
