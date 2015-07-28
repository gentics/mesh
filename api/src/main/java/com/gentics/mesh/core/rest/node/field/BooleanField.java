package com.gentics.mesh.core.rest.node.field;

public interface BooleanField extends ListableField, MicroschemaListableField {

	BooleanField setValue(Boolean value);

	Boolean getValue();

}
