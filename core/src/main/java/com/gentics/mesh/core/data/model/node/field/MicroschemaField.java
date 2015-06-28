package com.gentics.mesh.core.data.model.node.field;

import java.util.List;

public interface MicroschemaField extends Field {

	List<? extends MicroschemaListable> getFields();
}
