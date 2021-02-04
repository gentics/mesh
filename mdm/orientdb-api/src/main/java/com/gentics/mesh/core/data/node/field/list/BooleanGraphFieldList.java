package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;

public interface BooleanGraphFieldList extends HibBooleanFieldList, ListGraphField<HibBooleanField, BooleanFieldListImpl, Boolean> {

	String TYPE = "boolean";

}
