package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;

/**
 * @see StringGraphFieldList
 */
public class StringGraphFieldListImpl extends AbstractBasicGraphFieldList<HibStringField, StringFieldListImpl, String>
	implements StringGraphFieldList {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(StringGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public HibStringField createString(String string) {
		HibStringField field = createField();
		field.setString(string);
		return field;
	}

	@Override
	public HibStringField getString(int index) {
		return getField(index);
	}

	@Override
	protected StringGraphField createField(String key) {
		return new StringGraphFieldImpl(key, this);
	}

	@Override
	public Class<? extends StringGraphField> getListType() {
		return StringGraphFieldImpl.class;
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}
}
