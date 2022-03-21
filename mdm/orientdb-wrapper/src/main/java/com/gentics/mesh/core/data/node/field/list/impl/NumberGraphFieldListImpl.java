package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.impl.NumberGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;

/**
 * @see NumberGraphFieldList
 */
public class NumberGraphFieldListImpl extends AbstractBasicGraphFieldList<HibNumberField, NumberFieldListImpl, Number>
	implements NumberGraphFieldList {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(NumberGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public HibNumberField createNumber(Number number) {
		HibNumberField field = createField();
		field.setNumber(number);
		return field;
	}

	@Override
	public HibNumberField getNumber(int index) {
		return getField(index);
	}

	@Override
	protected NumberGraphField createField(String key) {
		return new NumberGraphFieldImpl(key, this);
	}

	@Override
	public Class<? extends HibNumberField> getListType() {
		return NumberGraphFieldImpl.class;
	}

	@Override
	public void delete(BulkActionContext context) {
		getElement().remove();
	}
}
