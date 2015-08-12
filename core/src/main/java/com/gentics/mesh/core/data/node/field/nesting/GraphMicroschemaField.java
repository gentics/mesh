package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;

public interface GraphMicroschemaField extends GraphNestingField, ListableGraphField {

	<T extends MicroschemaListableGraphField> List<? extends T> getFields();

}
