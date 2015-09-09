package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;

public interface MicroschemaGraphField extends NestingGraphField, ListableGraphField {

	<T extends MicroschemaListableGraphField> List<? extends T> getFields();

}
