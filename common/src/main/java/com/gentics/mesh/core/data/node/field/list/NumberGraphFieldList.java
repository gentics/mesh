package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;

public interface NumberGraphFieldList extends BasicListGraphField<NumberGraphField, NumberFieldListImpl, Number> {

	String TYPE = "number";

}
