package com.gentics.mesh.core.rest.node.field;

import java.util.List;


public interface MicroschemaField extends Field {

	List<? extends MicroschemaListableField> getFields();

}
