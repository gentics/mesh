package com.gentics.mesh.core.rest.node.field;

import com.gentics.mesh.core.rest.node.FieldMap;

/**
 * The HTML field stores plain HTML data.
 * Please note that {@link FieldMap} will handle the actual JSON format building.
 */
public interface HtmlField extends ListableField, MicroschemaListableField {

	/**
	 * Return the html field value.
	 * 
	 * @return Html value
	 */
	String getHTML();

	/**
	 * Set the html field value.
	 * 
	 * @param html
	 *            Html value
	 * @return Fluent API
	 */
	HtmlField setHTML(String html);

	@Override
	default Object getValue() {
		return getHTML();
	}
}
