package com.gentics.mesh.core.data.node.field.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.basic.BasicField;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.data.relationship.MeshRelationships;

public abstract class AbstractFieldList<T extends ListableField> extends MeshVertexImpl implements ListField<T> {

	@Override
	public void setFieldKey(String key) {
		// TODO Auto-generated method stub
	}

	@Override
	public String getFieldKey() {
		// TODO Auto-generated method stub
		return null;
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
