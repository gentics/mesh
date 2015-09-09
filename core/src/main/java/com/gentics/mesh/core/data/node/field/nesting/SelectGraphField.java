package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;

public interface SelectGraphField<T extends ListableGraphField> extends NestingGraphField, MicroschemaListableGraphField {

	void addOption(T t);

	List<T> getOptions();

	void removeOption(T t);

	void removeAllOptions();

	T getSelection();

	boolean isMultiselect();

	void setMultiselect(boolean flag);

	List<T> getSelections();
}
