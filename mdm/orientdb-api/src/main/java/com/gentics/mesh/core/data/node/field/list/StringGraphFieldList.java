package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;

/**
 * Domain model definition for a string list.
 */
public interface StringGraphFieldList extends ListGraphField<HibStringField, StringFieldListImpl, String>, HibStringFieldList {

	String TYPE = "string";

}
