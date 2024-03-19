package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.data.node.field.impl.BooleanGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;

/**
 * @see BooleanGraphFieldList
 */
public class BooleanGraphFieldListImpl extends AbstractBasicGraphFieldList<HibBooleanField, BooleanFieldListImpl, Boolean>
	implements BooleanGraphFieldList {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(BooleanGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public HibBooleanField getBoolean(int index) {
		return getField(index);
	}

	@Override
	public HibBooleanField createBoolean(Boolean flag) {
		HibBooleanField field = createField();
		field.setBoolean(flag);
		return field;
	}

	@Override
	protected BooleanGraphField createField(String key) {
		return new BooleanGraphFieldImpl(key, this);
	}

	@Override
	public Class<? extends BooleanGraphField> getListType() {
		return BooleanGraphFieldImpl.class;
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}
}
