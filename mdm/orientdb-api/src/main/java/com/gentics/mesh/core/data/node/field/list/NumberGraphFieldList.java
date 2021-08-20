package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;

/**
 * List field domain class for numbers.
 */
public interface NumberGraphFieldList extends ListGraphField<HibNumberField, NumberFieldListImpl, Number>, HibNumberFieldList {

	String TYPE = "number";

}
