package com.gentics.mesh.core.data.node.field.basic;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;

public interface NumberGraphField extends ListableGraphField, BasicGraphField {

	public void setNumber(String number);

	public String getNumber();
}
