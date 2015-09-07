package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.nesting.GraphMicroschemaField;
import com.gentics.mesh.core.rest.node.field.list.impl.MicroschemaFieldListImpl;

public interface GraphMicroschemaFieldList extends GraphListField<GraphMicroschemaField, MicroschemaFieldListImpl> {

	public static final String TYPE = "microschema";

}
