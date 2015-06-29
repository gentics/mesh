package com.gentics.mesh.core.data.model.node.field.basic;

import com.gentics.mesh.core.data.model.node.field.nesting.ListableField;

public interface DateField extends ListableField, BasicField {

	void setDate(String date);

	String  getDate();

}
