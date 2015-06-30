package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableField;

public interface DateField extends ListableField, BasicField {

	void setDate(String date);

	String  getDate();

}
