package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;

public interface HtmlGraphField extends ListableGraphField, BasicGraphField {

	void setHtml(String html);

	String getHTML();

}
