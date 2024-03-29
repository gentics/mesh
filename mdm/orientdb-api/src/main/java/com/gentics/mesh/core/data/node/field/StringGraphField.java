package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.StringField;

/**
 * The StringField Domain Model interface.
 */
public interface StringGraphField extends ListableGraphField, BasicGraphField<StringField>, DisplayField, HibStringField {

}
