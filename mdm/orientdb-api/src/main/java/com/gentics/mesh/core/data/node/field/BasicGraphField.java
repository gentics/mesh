package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;

/**
 * Marker interface for basic field definitions. Basic fields can also be included in micronodes.
 * 
 * @param <T>
 *            REST type of the field
 */
public interface BasicGraphField<T extends Field> extends MicroschemaListableGraphField, HibBasicField<T> {


}
