package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.NumberField;

/**
 * The NumberField Domain Model interface.
 * 
 * A number graph field is a basic node field which can be used to store a single number value.
 */
public interface NumberGraphField extends ListableGraphField, BasicGraphField<NumberField>, HibNumberField {

}
