package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;

public interface DateGraphFieldList extends BasicListGraphField<DateGraphField, DateFieldListImpl, Long> {

	String TYPE = "date";

}
