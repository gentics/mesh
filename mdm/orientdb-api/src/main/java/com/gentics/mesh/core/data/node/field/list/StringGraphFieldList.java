package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;

public interface StringGraphFieldList extends HibStringFieldList, ListGraphField<HibStringField, StringFieldListImpl, String> {

	String TYPE = "string";

}
