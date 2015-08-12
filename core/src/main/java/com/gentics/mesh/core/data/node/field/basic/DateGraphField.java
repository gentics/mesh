package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;

public interface DateGraphField extends ListableGraphField, BasicGraphField {

	void setDate(String date);

	String  getDate();

}
