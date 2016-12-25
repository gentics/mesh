package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;

public interface HtmlGraphFieldList extends ListGraphField<HtmlGraphField, HtmlFieldListImpl, String> {

	String TYPE = "html";

	/**
	 * Create a new html graph field.
	 * 
	 * @param html
	 * @return
	 */
	HtmlGraphField createHTML(String html);

	/**
	 * Return the html graph field at the given index position.
	 * 
	 * @param index
	 * @return
	 */
	HtmlGraphField getHTML(int index);

}
