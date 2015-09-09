package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.nesting.MicroschemaGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.MicroschemaFieldListImpl;

public interface MicroschemaGraphFieldList extends ListGraphField<MicroschemaGraphField, MicroschemaFieldListImpl> {

	public static final String TYPE = "microschema";

}
