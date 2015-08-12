package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.GraphField;
import com.syncleus.ferma.AbstractVertexFrame;

public abstract class AbstractBasicField implements GraphField {

	private String fieldKey;
	private AbstractVertexFrame parentContainer;

	public AbstractBasicField(String fieldKey, AbstractVertexFrame parentContainer) {
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

	public AbstractVertexFrame getParentContainer() {
		return parentContainer;
	}

	public void setFieldProperty(String key, String value) {
		parentContainer.setProperty(fieldKey + "-" + key, value);
	}

	public String getFieldProperty(String key) {
		return parentContainer.getProperty(fieldKey + "-" + key);
	}

}
