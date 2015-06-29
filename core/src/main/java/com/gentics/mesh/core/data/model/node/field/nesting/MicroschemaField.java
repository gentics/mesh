package com.gentics.mesh.core.data.model.node.field.nesting;

import java.util.List;

public interface MicroschemaField extends NestingField {

	<T extends MicroschemaListableField> List<? extends T> getFields();

}
