package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.impl.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;

/**
 * @see DateGraphFieldList
 */
public class DateGraphFieldListImpl extends AbstractBasicGraphFieldList<HibDateField, DateFieldListImpl, Long> implements DateGraphFieldList {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(DateGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public HibDateField createDate(Long date) {
		HibDateField field = createField();
		field.setDate(date);
		return field;
	}

	@Override
	protected DateGraphField createField(String key) {
		return new DateGraphFieldImpl(key, this);
	}

	@Override
	public HibDateField getDate(int index) {
		return getField(index);
	}

	@Override
	public Class<? extends DateGraphField> getListType() {
		return DateGraphFieldImpl.class;
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}
}
