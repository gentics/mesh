package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;

public abstract class AbstractFieldList<T extends ListableField> extends MeshVertexImpl implements ListField<T> {

	@Override
	public void setFieldKey(String key) {
		setProperty("fieldKey", key);
	}

	@Override
	public String getFieldKey() {
		return getProperty("fieldKey");
	}

	@Override
	public Class<T> getListType() {
		// TODO Auto-generated method stub
		return null;
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
