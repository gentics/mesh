package com.gentics.mesh.core.rest.node.field;

public interface HtmlField extends ListableField, MicroschemaListableField {

	/**
	 * Return the html field value.
	 * 
	 * @return
	 */
	String getHTML();

	/**
	 * Set the html field value.
	 * 
	 * @param html
	 * @return fluent API
	 */
	HtmlField setHTML(String html);

}
