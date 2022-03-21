package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;

public interface ListGraphField<T extends HibListableField, RM extends Field, U> extends HibListField<T, RM, U>, MicroschemaListableGraphField, MeshVertex {

}