package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;

public interface GraphNumberFieldList extends GraphListField<NumberGraphField> {

	public static final String TYPE = "number";

	NumberGraphField createNumber(String value);
	
	NumberGraphField getNumber(int index);

}
