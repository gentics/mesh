package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.BooleanField;

public interface BooleanFieldList extends ListField<BooleanField> {

	BooleanField getBoolean(int index);

	BooleanField createBoolean(Boolean flag);
}
