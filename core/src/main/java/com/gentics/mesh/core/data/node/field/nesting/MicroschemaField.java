package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;

public interface MicroschemaField extends NestingField, ListableField {

	<T extends MicroschemaListableField> List<? extends T> getFields();

}
