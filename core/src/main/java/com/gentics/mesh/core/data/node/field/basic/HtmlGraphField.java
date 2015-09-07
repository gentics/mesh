package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.HtmlField;

public interface HtmlGraphField extends ListableGraphField, BasicGraphField<HtmlField> {

	/**
	 * Set the html field value for the field.
	 * 
	 * @param html
	 */
	void setHtml(String html);

	/**
	 * Return the html field value for the field.
	 * 
	 * @return
	 */
	String getHTML();

}
