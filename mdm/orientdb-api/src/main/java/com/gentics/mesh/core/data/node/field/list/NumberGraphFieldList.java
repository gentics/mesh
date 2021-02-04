package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;

public interface NumberGraphFieldList extends HibNumberFieldList, ListGraphField<HibNumberField, NumberFieldListImpl, Number> {

	String TYPE = "number";

}
