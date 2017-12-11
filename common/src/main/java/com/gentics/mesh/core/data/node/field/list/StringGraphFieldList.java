package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;

public interface StringGraphFieldList extends BasicListGraphField<StringGraphField, StringFieldListImpl, String> {

	String TYPE = "string";


}
