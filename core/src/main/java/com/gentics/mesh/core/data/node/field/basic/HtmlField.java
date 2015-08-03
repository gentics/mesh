package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableField;

public interface HtmlField extends ListableField, BasicField {

	void setHtml(String html);

	String getHTML();

}
