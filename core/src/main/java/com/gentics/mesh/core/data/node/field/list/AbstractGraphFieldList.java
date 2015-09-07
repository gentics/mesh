package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;

public abstract class AbstractGraphFieldList<T extends ListableGraphField, RM extends Field> extends MeshVertexImpl implements GraphListField<T, RM> {

	@Override
	public void setFieldKey(String key) {
		setProperty(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	public String getFieldKey() {
		return getProperty(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public void addItem(T item) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeItem(T item) {
		// TODO Auto-generated method stub

	}

}
