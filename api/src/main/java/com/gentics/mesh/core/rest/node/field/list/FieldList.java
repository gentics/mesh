package com.gentics.mesh.core.rest.node.field.list;

import java.util.List;

import com.gentics.mesh.core.rest.node.field.Field;

public interface FieldList<T> extends Field {

//	void setTotalCount(long totalCount);

//	long getTotalCount();

	void add(T field);

	List<T> getItems();

	void setItems(List<T> items);

}
