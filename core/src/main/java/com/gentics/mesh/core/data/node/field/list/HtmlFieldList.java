package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.HtmlField;

public interface HtmlFieldList extends ListField<HtmlField> {

	HtmlField createHTML(String key);
	
	HtmlField getHTML(int index);

}
