package com.gentics.mesh.core.data.model.node.field.nesting;

import java.util.List;

public interface ListField<T extends ListableField> extends NestingField {

	List<? extends T> getList();

	void setListType(Class<? extends T> t);

	Class<? extends T> getListType();

}
