package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.DateField;

/**
 * The DateField Domain Model interface.
 * 
 * A date graph field is a basic node field which can be used to store a single date value.
 */
public interface DateGraphField extends ListableGraphField, BasicGraphField<DateField>, HibDateField {

}
