package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;

public interface BasicGraphField<T extends Field> extends HibBasicField<T>, MicroschemaListableGraphField {

}
