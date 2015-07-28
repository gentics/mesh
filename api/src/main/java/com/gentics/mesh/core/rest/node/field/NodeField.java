package com.gentics.mesh.core.rest.node.field;

public interface NodeField extends ListableField, MicroschemaListableField {

	String getUuid();

	NodeField setUuid(String uuid);

}
