package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.rest.node.field.Field;
import com.syncleus.ferma.AbstractVertexFrame;

/**
 * Abstract class for basic fields. All basic fields should implement this class in order to provide various methods that can be used to access basic field values.
 * 
 * A basic graph field is a field which is not stored within its own vertex or edge. Instead the field properties are stored next to it's parent container. A node string field for
 * example are stored within the {@link NodeGraphFieldContainer} vertex. This way no additional graph traversal is needed to load such basic fields.
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
		setFieldProperty("field", key);
	}

	/**
	 * Return the parent container which holds the properties for the field.
	 * 
	 * @return Parent container (micronode container or node graph field container)
	 */
	public AbstractVertexFrame getParentContainer() {
		return parentContainer;
	}

	/**
	 * Set the parent container for the field.
	 * 
	 * @param key
	 * @param value
	 */
	public void setFieldProperty(String key, Object value) {
		parentContainer.setProperty(fieldKey + "-" + key, value);
	}

	/***
	 * Return the basic field property using the given key.
	 * 
	 * @param key
	 * @return
	 */
	public <E> E getFieldProperty(String key) {
		return parentContainer.getProperty(fieldKey + "-" + key);
	}

	@Override
	public void validate() {
	}
}
