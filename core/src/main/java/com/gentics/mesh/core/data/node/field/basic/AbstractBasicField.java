package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.ActionContext;
import com.syncleus.ferma.AbstractVertexFrame;

public abstract class AbstractBasicField<T extends Field> implements BasicGraphField<T> {

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

	/**
	 * Transform the field into the rest response model.
	 */
	abstract public T transformToRest(ActionContext ac);

}
