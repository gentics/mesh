package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.ActionContext;
import com.syncleus.ferma.AbstractVertexFrame;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * Abstract class for basic fields. All basic fields should implement this class in order to provide various methods that can be used to access basic field
 * values.
 */
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

	public void setFieldProperty(String key, Object value) {
		parentContainer.setProperty(fieldKey + "-" + key, value);
	}

	public <T> T getFieldProperty(String key) {
		return parentContainer.getProperty(fieldKey + "-" + key);
	}

	/**
	 * Transform the field into the rest response model.
	 */
	abstract public void transformToRest(ActionContext ac, Handler<AsyncResult<T>> handler);

}
