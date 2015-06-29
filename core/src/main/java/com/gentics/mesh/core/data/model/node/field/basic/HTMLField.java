package com.gentics.mesh.core.data.model.node.field.basic;

import com.gentics.mesh.core.data.model.node.field.nesting.ListableField;

public interface HTMLField extends ListableField, BasicField {

	void setHTML(String html);

	String getHTML();

}
