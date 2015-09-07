package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;

public interface GraphNumberFieldList extends GraphListField<NumberGraphField, NumberFieldListImpl> {

	public static final String TYPE = "number";

	NumberGraphField createNumber(String value);

	NumberGraphField getNumber(int index);

}
