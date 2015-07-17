package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.NumberField;

public interface NumberFieldList extends ListField<NumberField> {

	NumberField createNumber(String value);
	
	NumberField getNumber(int index);

}
