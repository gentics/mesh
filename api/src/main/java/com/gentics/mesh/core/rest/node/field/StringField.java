package com.gentics.mesh.core.rest.node.field;

public interface StringField extends ListableField, MicroschemaListableField {

	String getText();

	void setText(String text);

}
