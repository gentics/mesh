package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;

public interface HibHtmlFieldList extends HibMicroschemaListableField, HibListField<HibHtmlField, HtmlFieldListImpl, String> {

	/**
	 * Create a new html graph field.
	 * 
	 * @param html
	 * @return
	 */
	HibHtmlField createHTML(String html);

	/**
	 * Return the html graph field at the given index position.
	 * 
	 * @param index
	 * @return
	 */
	HibHtmlField getHTML(int index);

}
