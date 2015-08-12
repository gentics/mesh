package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;

public interface GraphSelectField<T extends ListableGraphField> extends GraphNestingField, MicroschemaListableGraphField {

	void addOption(T t);

	List<T> getOptions();

	void removeOption(T t);

	void removeAllOptions();

	T getSelection();

	boolean isMultiselect();

	void setMultiselect(boolean flag);

	List<T> getSelections();
}
