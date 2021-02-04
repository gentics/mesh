package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;

public interface DateGraphFieldList extends HibDateFieldList, ListGraphField<HibDateField, DateFieldListImpl, Long> {

	String TYPE = "date";
}
