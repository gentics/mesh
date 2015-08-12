package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;

public interface GraphHtmlFieldList extends GraphListField<HtmlGraphField> {

	public static final String TYPE = "html";

	HtmlGraphField createHTML(String key);
	
	HtmlGraphField getHTML(int index);

}
