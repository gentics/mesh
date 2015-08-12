package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.DateGraphField;

public interface GraphDateFieldList extends GraphListField<DateGraphField> {

	public static final String TYPE = "date";

	DateGraphField createDate(String key);

	DateGraphField getDate(int index);
}
