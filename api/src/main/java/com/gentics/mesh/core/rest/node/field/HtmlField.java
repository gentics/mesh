package com.gentics.mesh.core.rest.node.field;

public interface HtmlField extends ListableField, MicroschemaListableField {

	String getHTML();

	HtmlField setHTML(String html);

}
