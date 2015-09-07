package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;

public interface GraphHtmlFieldList extends GraphListField<HtmlGraphField, HtmlFieldListImpl> {

	public static final String TYPE = "html";

	HtmlGraphField createHTML(String key);

	HtmlGraphField getHTML(int index);

}
