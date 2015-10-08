package com.gentics.mesh.core.rest.node;

import java.util.Map;

import com.gentics.mesh.core.rest.node.field.Field;

/**
 * Convenience class which is used to classify a field map so that it can be referenced within the custom json deserializers.
 *
 */
public interface FieldMap extends Map<String, Field> {

}
