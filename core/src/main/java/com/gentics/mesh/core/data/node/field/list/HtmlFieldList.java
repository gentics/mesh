package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.HtmlField;

public interface HtmlFieldList extends ListField<HtmlField> {

	public static final String TYPE = "html";

	HtmlField createHTML(String key);
	
	HtmlField getHTML(int index);

}
