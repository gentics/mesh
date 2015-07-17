package com.gentics.mesh.core.rest.node.field;

public interface HTMLField extends ListableField, MicroschemaListableField {

	String getHTML();

	HTMLField setHTML(String html);

}
