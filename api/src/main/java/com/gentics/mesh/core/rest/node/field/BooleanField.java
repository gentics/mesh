package com.gentics.mesh.core.rest.node.field;

public interface BooleanField extends ListableField, MicroschemaListableField {

	void setValue(Boolean value);

	Boolean getValue();

}
