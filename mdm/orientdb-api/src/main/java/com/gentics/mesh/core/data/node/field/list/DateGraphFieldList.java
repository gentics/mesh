package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;

/**
 * Domain model definition for a date list.
 */
public interface DateGraphFieldList extends ListGraphField<HibDateField, DateFieldListImpl, Long>, HibDateFieldList {

	String TYPE = "date";

}
