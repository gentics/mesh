package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.HtmlField;

public interface HibHtmlField extends HibListableField, HibBasicField<HtmlField> {

	/**
	 * Set the HTML field value for the field.
	 * 
	 * @param html
	 */
	void setHtml(String html);

	/**
	 * Return the HTML field value for the field.
	 * 
	 * @return
	 */
	String getHTML();

}
