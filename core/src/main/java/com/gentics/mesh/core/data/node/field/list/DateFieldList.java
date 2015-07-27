package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.DateField;

public interface DateFieldList extends ListField<DateField> {

	public static final String TYPE = "date";

	DateField createDate(String key);

	DateField getDate(int index);
}
