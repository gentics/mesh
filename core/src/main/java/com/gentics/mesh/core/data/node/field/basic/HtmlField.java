package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableField;

public interface HtmlField extends ListableField, BasicField {

	void setHTML(String html);

	String getHTML();

}
